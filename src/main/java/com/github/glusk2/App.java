package com.github.glusk2;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;

import javafx.application.Application;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
                Thread.currentThread()
                      .getContextClassLoader()
                      .getResource("dtecbs-l.xml")
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
        new App().launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        List<CheckBox> cbListCurrency = new ArrayList<CheckBox>();
        Set<String> valute =
            new HashSet<String>(
                xml.xpath("//bsi:tecajnica/bsi:tecaj/@oznaka")
            );
        for (String oznaka : valute) {
            CheckBox cb = new CheckBox(oznaka);
            cbListCurrency.add(cb);
            cb.setMinWidth(100);
        }
        DatePicker dpFrom = new DatePicker(LocalDate.now().minusYears(1));
        DatePicker dpTo = new DatePicker(LocalDate.now());

        final HBox rootPane =
            new HBox(
                new VBox(
                    5,
                    new Label("Valute:"),
                    new ScrollPane(
                        new VBox(
                            5,
                            cbListCurrency.toArray(new CheckBox[0])
                        )
                    ),
                    new Label("Od:"),
                    dpFrom,
                    new Label("Do:"),
                    dpTo
                )
                // area chart insert point
            );
        EventHandler<ActionEvent> eh = (event) -> {
            LocalDate fromDate = dpFrom.getValue();
            LocalDate toDate = dpTo.getValue();

            long lowerBound = fromDate.toEpochDay();
            long upperBound = toDate.toEpochDay();

            long daysDelta = ChronoUnit.DAYS.between(fromDate, toDate);
            if (daysDelta < 0) {
                Alert alert = new Alert(
                    Alert.AlertType.WARNING,
                    "Datum začetka tečaja je za datumom konca tečaja!",
                    ButtonType.OK
                );
                alert.showAndWait();
                dpFrom.setValue(dpTo.getValue().minusDays(30));
                return;
            }
            long tickUnit = daysDelta < 31 ? 1 : (long)Math.ceil(daysDelta / 12);

            NumberAxis xAxis = new NumberAxis(lowerBound, upperBound, tickUnit);
            xAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    return LocalDate.ofEpochDay(object.longValue()).format(chartDateFormat);
                }

                @Override
                public Number fromString(String string) {
                    return LocalDate.parse(string, chartDateFormat).toEpochDay();
                }
            });
            xAxis.setTickLabelRotation(45);

            NumberAxis yAxis = new NumberAxis();
            AreaChart<Number,Number> ac = new AreaChart<Number,Number>(xAxis,yAxis);
            ac.setTitle("Tečaji");
            ac.setMinWidth(1000);

            List<CheckBox> selected = new ArrayList<CheckBox>();
            for (CheckBox cb : cbListCurrency) {
                if (cb.isSelected()) {
                    selected.add(cb);
                }
            }

            List<XML> tecajnicas = xml.nodes(
                String.format(
                    "//bsi:tecajnica[number(translate(@datum, '-', '')) >= %s and number(translate(@datum, '-', '')) <= %s]",
                    fromDate.format(xmlComparableDateFormat),
                    toDate.format(xmlComparableDateFormat)
                )
            );

            for (CheckBox cb : selected) {
                XYChart.Series nextSeries= new XYChart.Series();
                nextSeries.setName(cb.getText());
                for (XML tecajnica : tecajnicas) {
                    XML clonedTecajnica =
                        new XMLDocument(
                            tecajnica.toString()
                        ).registerNs("bsi", "http://www.bsi.si");

                    Long datum =
                        LocalDate.parse(
                            clonedTecajnica.xpath("/bsi:tecajnica/@datum").get(0),
                            dbDateFormat
                        ).toEpochDay();

                    List<String> tecajQuery =
                        clonedTecajnica.xpath(
                            String.format(
                                "/bsi:tecajnica/bsi:tecaj[@oznaka=\"%s\"]/text()",
                                cb.getText()
                            )
                        );
                    if (tecajQuery.isEmpty()) {
                        continue;
                    }

                    nextSeries.getData().add(
                        new XYChart.Data(
                            datum,
                            Double.parseDouble(tecajQuery.get(0))
                        )
                    );
                }
                ac.getData().addAll(nextSeries);
            }
            ac.setCreateSymbols(false);
            if (rootPane.getChildren().size() > 1) {
                rootPane.getChildren().remove(1);
            }
            rootPane.getChildren().add(ac);
        };
        dpFrom.setOnAction(eh);
        dpTo.setOnAction(eh);
        for (CheckBox cb : cbListCurrency) {
            cb.setOnAction(eh);
            if (cb.getText().equals("USD")) {
                cb.fire();
            }
        }

        rootPane.setPadding(new Insets(10, 10, 10, 10));
        primaryStage.setTitle("Tečajnica");
        primaryStage.setScene(new Scene(rootPane));
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }
}
