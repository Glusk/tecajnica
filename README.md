# Tečajnica

[![Build Status](https://travis-ci.com/Glusk/tecajnica.svg?branch=master)](https://travis-ci.com/Glusk/tecajnica)

Moja rešitev testne naloge podjetja [irose](https://www.i-rose.si/).

## Naloga

Napiši program, ki za poljubno izbrano časovno obdobje iz podatkovne baze, izpiše tečajnico za izbrane valute, poleg tabelaričnega izpisa naj se izriše še graf.
Dodatna funkcionalnost naj bo še izračun oportunitetnih zaslužkov/izgub med dvema poljubno izbranima valutama za poljubno izbrani termin iz podatkovne baze.

Kot vir vrednosti podatkovne baze naj se uporabi tečajna lista BSI (Prenos časovnih serij od 2007 http://www.bsi.si/_data/tecajnice/dtecbs-l.xml)

Program naj bo napisan v programskem jeziku Java, brez nepotrebnih odvisnosti, vsebovati mora prevajalno skripto, zaželjen je Maven, lahko tudi ANT. Program mora biti zapakiran v arhivsko datoteko, ki omogoča uporabniku prijazno izvajanje.

## Releases

Use the [release](./release.sh) script with the following arguments:

1.  `release` - the next release version

2.  `snapshot` - the next snapshot version

3.  `dryRun` (optional) - if set to `true`, the changes will not be pushed
   to the remote repository

Example:

``` bash
./release.sh 0.1.1 0.1.2-SNAPSHOT
```
