README-ncaa-data
================

# About

This SQL directory contains CSV data files and example SQL for generating
staging tables for NCAA basketball datasets.

* cbb21.csv - An NCAA Division 1 dataset captured from kaggle.com: 
  https://www.kaggle.com/datasets/andrewsundberg/college-basketball-dataset
  This file contains data relevant to the team standings at the end of the
  2021 season. We used it as a datasource for conference names, team names,
  conference / team mappings, etc.
* state-abbrevs.csv - A dataset of US state names and abbreviations
  captured from kaggle.com:
  https://www.kaggle.com/datasets/giodev11/usstates-dataset
* ncaa-conferences.csv - NCAA conference data distilled from the cbb21.csv
  dataset.
* ncaa-teams.csv - NCAA team data distilled from the cbb21.csv dataset
* ncaa-states.csv - NCAA state data distilled from state-abbrevs.csv

See `ncaa-data.sql` for table creation and data generation queries

----

