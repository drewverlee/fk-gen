(defproject fk-gen "0.1.0-alpha"
  :description "Generates insert statements for a postgres table and all its foreign key dependencies."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.layerware/hugsql "0.4.8"]
                 [org.postgresql/postgresql "42.2.2"]
                 [org.clojure/java.jdbc "0.7.5"]
                 [viesti/table-spec "0.1.1"]
                 [nilenso/honeysql-postgres "0.2.3"]
                 [org.clojure/test.check "0.9.0"]]
  :profiles {:dev {:dependencies  [[com.opentable.components/otj-pg-embedded "0.11.4"]]}}) 
