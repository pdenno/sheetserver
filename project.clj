(defproject pdenno/sheetserver "0.1.0"
  :description "Exporatory code to generate a 'sheetserver'"
  :url "https://github.com/pdenno/sheetserver"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure       "1.10.1"]
                 [org.clojure/data.json     "0.2.6" ]
                 [org.clojure/tools.logging "0.4.1" ]
                 [http-kit                  "2.3.0" ]
                 [metosin/reitit            "0.3.9" ]
                 [dk.ative/docjure          "1.13.0"]]
  :main ^:skip-aot pdenno.sheetserver.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

