(ns fk-gen.core
  (:require [clojure.set :as set]
            [hugsql.core :as hugsql]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [honeysql-postgres.format :refer :all]
            [honeysql-postgres.helpers :refer :all]
            [clojure.java.jdbc :as j]
            [table-spec.core :as t]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [com.rpl.specter :refer [transform MAP-VALS ALL]]
            [clojure.string :as str]))

;; defines get-fk-deps
(hugsql/def-db-fns "fk-deps.sql")

;;TODO make sure handling the case of no deps probably merge or something or change sql? graph should have everything
(defn- fk-deps->graph
  [fk-deps]
  (reduce-kv (fn [c fk v]
               (assoc c fk (reduce
                            (fn [x {:keys [fk-table pk-table fk-column pk-column]}]
                              (assoc x pk-table {:fk-column fk-column :pk-column pk-column}))
                            {} v)))
             {} (group-by :fk-table (transform [ALL MAP-VALS] keyword fk-deps))))

(defn- dfs->path
  ([n f g]
   (dfs->path [n] f #{} g))
  ([nxs f v g]
   (let [n (peek nxs)
         v (conj v n)]
     (when n (cons (f n g) (dfs->path (filterv #(not (v %)) (concat (pop nxs) (keys (g n)))) f v g))))))

(defn- fk-deps->sql-plan
  [fk-deps table table-graph->insert-stmt-plan]
  (->> fk-deps
      tables->graph
      (dfs->path table table-graph->insert-stmt-plan)
      reverse))


(s/def ::table keyword?)
;;(s/def ::db-info )

;;TODO add spec
;;TODO should i pass db-info to everything or so they can query it?
(defn create
 "Returns a vector of insert statements necessary to fulfill all the foreign key constraints of the given table
  table   :keyword : the root of the dependency tree graph you want to generate
  db-info :hashmap : a map describing the database connection information"
  [table db-info]
  (->> db-info
       get-fk-deps
       fk-deps->sql-plan))



