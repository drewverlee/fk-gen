# fk-gen

This library contains functions for 
generates insert statements for a table and all its dependencies.

This is probably best explained with an example.

Lets say you have a postgres schema that includes two tables

dogs 
persons

There is a foriegn key constraint that dogs have an owner which is a person

dog -> persons

We can say that dogs depends on persons. That is, in order for a dog to exist,
a person has to exist.

What this library does is creates insert statements for a given table
and all its dependencies

"insert into persons cols (..) value (...)"
"insert into dogs cols (...) values (...)"

in such a way that the foriegn key constraint is meet.

I imagine this will be useful in testing, where often times i find
myself having to fill in databases tables i dont care about to test 
the parts i do.


# tests

`lein test`

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
