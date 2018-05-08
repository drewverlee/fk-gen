;; ## Value proposition
;; You should only have to care about the data in the database that you ... care about.
;; But often, you have to care about more because the database has some constraints that you have to meet.
;; This library handles *some* of those constraints for you by generating data that satisfies them.

;; ## Introducing "Foreign Key Generator" or fk-gen for short
;; This library helps generate sql statments that fulfill two constraints
;;
;; 1. data types  : e.g column's values are of type int
;; 2. foreign key : column's values match another value in another table's column

;; ## Using the library

;; It does this with only a little help from you. Mainly you have to tell it:
;;
;; 1. How to connect to the database. So host, port, etc..
;; 2. Which table you wish to generate insert statments for.
;; 3. A function for how to handle the foreign key dependencies
;;
;; Don't worry about the third point (the function) will provide one of those for you.


;; ### Example
;; Lets say you have a database with two tables
;;
;; 1. dogs
;; 2. persons

;; where a dog has to have a owner. A _constraint_ that is enforced through a foreign key dependency
;;
;; dogs -> persons

;; When you use this library you can expect to get sql that full fills this constraint. E.g a list of sql insert statments like...

;; 1. "insert into persons (id) values (101)"
;; 2. "insert into dogs (id, person) values (1, 101)"

;; The clojure code for doing this looks like you might expect:

;; <pre><code>
;; (->sql-and-insert! {:table :dogs 
;;                     :db-info db-info)
;; </code></pre>
;;
;; Inserts sql insert statements that full fill all the foreign key dependencies of the table.
;; I suggest jumping to either the test (core_test.clj) for a working example or the fk-gen.generate namespace for the public facing functionality
;;
(ns fk-gen.core
  "Contains all the functionality to get and transform the foreign key dependencies"
  (:require [clojure.set :as set]
            [hugsql.core :as hugsql]
            [com.rpl.specter :refer [transform MAP-VALS ALL]]))



;; ## Understanding how the library works
;; What follows is a overview of how this library works internally and so can be happily ignored if your just a consumer of the functionality.

;; ### Get the foreign key dependencies
;; This is made possible by first extracting foreign key information from the database
;; through a sql function which we can call called `get-fk-deps` which is brought into our namespace here
(hugsql/def-db-fns "fk-deps.sql")

;; ### Transform foreign key dependencies into a graph
;; Once we have the foreign key dependencies we need to put them into a structure that is easy to traverse so we turn it into a graph.
(defn- fk-deps->graph
  [fk-deps]
  (reduce-kv (fn [c fk v]
               (assoc c fk (reduce
                            (fn [x {:keys [fk-table pk-table fk-column pk-column]}]
                              (assoc x pk-table {:fk-column fk-column :pk-column pk-column}))
                            {} v)))
             {} (group-by :fk-table (transform [ALL MAP-VALS] keyword fk-deps))))

;; ### Transform the graph into our sql insert statments
;; Given the graph with the structure like:
;; <pre><code>
;; {:dogs {:persons {:fk-column :owner 
;;                   :pk-column :id}}}
;; </code></pre>

;;we can do a depth first search walk on it and on each node (table) use our table-graph->sql->insert-stmt-plan function which takes the
;; current table and the graph and produces a sql insert statments

;; Sense these sql insert statments are ordered by walking our dependency graph, we can simply reverse that ordering and insert them to full fill the database
;; constraints.
(defn- graph->dfs-path
  ([n f g]
   (graph->dfs-path [n] f #{} g))
  ([nxs f v g]
   (let [n (peek nxs)
         v (conj v n)]
     (when n (cons (f n g)
                   (graph->dfs-path
                    (filterv #(not (v %))
                             (concat (pop nxs) (keys (g n))))
                    f v g))))))

;; We wrap that functionality together into a side effect free function
(defn fk-deps->sql-plan
  [{:keys [table table-graph->insert-stmt-plan fk-deps]}]
  (->> fk-deps
       fk-deps->graph
       (graph->dfs-path table table-graph->insert-stmt-plan)
       reverse))
