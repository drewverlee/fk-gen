(defproject fk-gen "0.1.0-alpha"
  :description "Generates insert statements for a postgres table and all its foreign key dependencies."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.layerware/hugsql "0.4.8"]
                 [org.postgresql/postgresql "42.2.2"]
                 [com.rpl/specter "1.1.1"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [viesti/table-spec "0.1.1"]
                 [nilenso/honeysql-postgres "0.2.3"]
                 [datascript "0.16.4"]
                 [org.clojure/test.check "0.9.0"]]
  :profiles {:dev {:dependencies  [[com.opentable.components/otj-pg-embedded "0.12.0"]]}}) 
