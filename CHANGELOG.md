# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [0.3.0-alpha] - 2018-04-20

- Added a generators namespace that provides `->sql-plan`, `->sql`, `->sql-and-insert!` functions that are public facing. While they provide a good
amount of functionality, their fairly closed and should be also considered examples of what can be done.

## [0.2.0-alpha] - 2018-04-20

- Modified the project so the table-graph->insert-sql-plan function is now provided by the user. Meaning,
the user of this library has to provide one. Which makes it easy for them to customize the generators.

## [0.1.0-alpha] - 2018-04-20
### Added

- Added ability to generate table data and all its foreign key dependencies.
