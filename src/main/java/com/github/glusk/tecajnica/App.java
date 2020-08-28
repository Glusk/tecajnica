package com.github.glusk.tecajnica;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class App extends Application {

    private final XML xml;
    private final DateTimeFormatter dbDateFormat;
    private final DateTimeFormatter chartDateFormat;
    private final DateTimeFormatter xmlComparableDateFormat;

    public App() throws Exception {
        this(
            new XMLDocument(
                new URL("https://www.bsi.si/_data/tecajnice/dtecbs-l.xml")
            )
            .registerNs("bsi", "http://www.bsi.si"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d. MMM yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMdd")
        );
    }

    public App(
        XML xml,
        DateTimeFormatter dbDateFormat,
        DateTimeFormatter chartDateFormat,
        DateTimeFormatter xmlComparableDateFormat
    ) {
        this.xml = xml;
        this.dbDateFormat = dbDateFormat;
        this.chartDateFormat = chartDateFormat;
        this.xmlComparableDateFormat = xmlComparableDateFormat;
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(new Locale("sl"));
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<CheckBox> currencies =
               xml.xpath("//bsi:tecajnica/bsi:tecaj/@oznaka")
              .parallelStream()
              .distinct()
              .sorted()
              .map(oznaka -> new CheckBox(oznaka))
              .collect(Collectors.toList());
        DatePicker dpFrom = new DatePicker(LocalDate.now().minusYears(1));
        DatePicker dpTo = new DatePicker(LocalDate.now());

        DatePicker dpRates = new DatePicker(LocalDate.now());
        dpRates.setMaxWidth(Double.MAX_VALUE);

        BorderPane rootPane =
            new BorderPane(
                null,
                null,
                null,
                null,
                new VBox(
                    5,
                    new Label("Valute:"),
                    new ScrollPane(
                        new VBox(
                            5,
                            currencies.toArray(new CheckBox[0])
                        )
                    ),
                    new Label("Od:"),
                    dpFrom,
                    new Label("Do:"),
                    dpTo
                )
            );

        EventHandler<ActionEvent> chartDrawingHandler = (event) -> {
            LocalDate fromDate = dpFrom.getValue();
            LocalDate toDate = dpTo.getValue();

            long daysDelta = ChronoUnit.DAYS.between(fromDate, toDate);
            if (daysDelta < 0) {
                new Alert(
                    Alert.AlertType.WARNING,
                    "Datum začetka tečaja je za datumom konca tečaja!",
                    ButtonType.OK
                ).showAndWait();
                dpFrom.setValue(dpTo.getValue().minusDays(30));
                return;
            }
            long tickUnit =
                daysDelta < 31 ? 1 : (long) Math.ceil(daysDelta / 12);

            NumberAxis xAxis =
                new NumberAxis(
                    fromDate.toEpochDay(),
                    toDate.toEpochDay(),
                    tickUnit
                );
            xAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    return LocalDate.ofEpochDay(
                        object.longValue()).format(chartDateFormat);
                }
                @Override
                public Number fromString(String string) {
                    return LocalDate.parse(
                        string, chartDateFormat).toEpochDay();
                }
            });
            xAxis.setTickLabelRotation(45);

            AreaChart<Number,Number> ac =
                new AreaChart<Number,Number>(
                    xAxis,
                    new NumberAxis()
                );
            ac.setTitle("Tečaji");
            ac.setPadding(new Insets(10, 30, 10, 30));

            List<String> selectedCurrencies = currencies.stream()
                .filter(cb -> cb.isSelected())
                .map(c -> c.getText())
                .collect(Collectors.toList());

            List<XML> tecajnicas = xml.nodes(
                String.format(
                    "//bsi:tecajnica[number(translate(@datum, '-', '')) >= %s and number(translate(@datum, '-', '')) <= %s]",
                    fromDate.format(xmlComparableDateFormat),
                    toDate.format(xmlComparableDateFormat)
                )
            );

            Map<String, XYChart.Series> chartSeries =
                new TreeMap<String, XYChart.Series>();
            for (String currency : selectedCurrencies) {
                XYChart.Series series = new XYChart.Series();
                series.setName(currency);
                chartSeries.put(currency, series);
            }
            for (XML tecajnica : tecajnicas) {
                XML clonedTecajnica =
                    new XMLDocument(
                        tecajnica.toString()
                    ).registerNs("bsi", "http://www.bsi.si");

                LocalDate datum =
                    LocalDate.parse(
                        clonedTecajnica.xpath("/bsi:tecajnica/@datum").get(0),
                        dbDateFormat
                    );
                for (String currency : selectedCurrencies) {
                    List<String> tecajQuery =
                        clonedTecajnica.xpath(
                            String.format(
                                "/bsi:tecajnica/bsi:tecaj[@oznaka=\"%s\"]/text()",
                                currency
                            )
                        );
                    if (tecajQuery.isEmpty()) {
                        continue;
                    }

                    chartSeries.get(currency).getData().add(
                        new XYChart.Data(
                            datum.toEpochDay(),
                            Double.parseDouble(tecajQuery.get(0))
                        )
                    );
                }
            }

            for (String currency : chartSeries.keySet()) {
                ac.getData().add(chartSeries.get(currency));
            }
            ac.setCreateSymbols(false);
            rootPane.setCenter(ac);
        };

        EventHandler<ActionEvent> currencyRatesHandler = (event) -> {
            LocalDate rateDate = dpRates.getValue();

            List<String> selectedCurrencies = currencies.stream()
                .filter(cb -> cb.isSelected())
                .map(c -> c.getText())
                .collect(Collectors.toList());

            List<XML> tecajnicas = xml.nodes(
                String.format(
                    "//bsi:tecajnica[number(translate(@datum, '-', '')) <= %s]",
                    rateDate.format(xmlComparableDateFormat)
                )
            );
            if (!tecajnicas.isEmpty()) {
                XML tecajnica = tecajnicas.get(tecajnicas.size() - 1);
                XML clonedTecajnica =
                    new XMLDocument(
                        tecajnica.toString()
                    ).registerNs("bsi", "http://www.bsi.si");

                TableView table = new TableView();
                ObservableList<ObservableList<String>> data =
                    FXCollections.observableArrayList();
                TableColumn<ObservableList<String>, String> valutaCol =
                    new TableColumn<ObservableList<String>, String>("Valuta");
                valutaCol.setCellValueFactory(
                    param -> new ReadOnlyObjectWrapper<>(param.getValue().get(0))
                );
                TableColumn<ObservableList<String>, String> tecajCol =
                    new TableColumn<ObservableList<String>, String>("Tečaj");
                tecajCol.setCellValueFactory(
                    param -> new ReadOnlyObjectWrapper<>(param.getValue().get(1))
                );
                table.getColumns().addAll(valutaCol, tecajCol);
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

                for (String currency : selectedCurrencies) {
                    List<String> tecajQuery =
                        clonedTecajnica.xpath(
                            String.format(
                                "/bsi:tecajnica/bsi:tecaj[@oznaka=\"%s\"]/text()",
                                currency
                            )
                        );
                    if (tecajQuery.isEmpty()) {
                        continue;
                    }
                    data.add(
                        FXCollections.observableArrayList(
                            currency,
                            tecajQuery.get(0)
                        )
                    );
                }
                table.setItems(data);

                Label tableTitle = new Label("Tečajnica:");
                tableTitle.setFont(new Font("Arial", 16));

                VBox tablePane = new VBox(10, tableTitle, dpRates, table);
                tablePane.setPadding(new Insets(10, 10, 10, 10));

                rootPane.setRight(tablePane);
            }
        };

        dpFrom.setOnAction(chartDrawingHandler);
        dpTo.setOnAction(chartDrawingHandler);
        for (CheckBox cb : currencies) {
            cb.addEventHandler(ActionEvent.ACTION, chartDrawingHandler);
            cb.addEventHandler(ActionEvent.ACTION, currencyRatesHandler);
            if (cb.getText().equals("USD")) {
                cb.fire();
            }
        }
        dpRates.setOnAction(currencyRatesHandler);

        rootPane.setPadding(new Insets(10, 10, 10, 10));
        primaryStage.setTitle("Tečajnica");
        primaryStage.setScene(new Scene(rootPane));
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(800);
        primaryStage.show();
    }
}
