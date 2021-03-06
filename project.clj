(defproject fk-gen "0.3.3-alpha"
  :description "Generates insert statements for a postgres table and all its foreign key dependencies."
  :url "https://github.com/drewverlee/fk-gen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.layerware/hugsql "0.4.8"]
                 [drewverlee/table-spec "0.1.3"]
                 [nilenso/honeysql-postgres "0.2.3"]
                 [com.rpl/specter "1.1.1"]
                 [org.clojure/test.check "0.9.0"]]
  :profiles {:dev {:dependencies  [[com.opentable.components/otj-pg-embedded "0.12.0"]]}}
  :plugins [[lein-marginalia "0.9.1"]])
