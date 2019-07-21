(ns pdenno.sheetserver.main
  (:require
   [clojure.string          :as str]
   [pdenno.sheetserver.core :as core])
  (:gen-class))

(def arg2key {"--port" :port
              "--mapping" :mapping
              "--schema"  :schema
              "--spreadsheet" :spreadsheet
              "--sheet" :sheet})

(def valid-arg? (-> arg2key keys set))

(defn usage []
  (println "Usage: java -jar <name of standalone.jar> [--port <port num>]") 
  (println "                                          [--mapping <mapping file>]")
  (println "                                          [--schema <schema file>]")
  (println "                                          [--spreadsheet <spreadsheet file>]")
  (println "                                          [--sheet <name of sheet in spreasheet file>]")
  (println "")
  (println "Any of the optional arguments not specified defaults to the example usage in the resources directory.")
  (println "Typical usage is one of (1) run the example:(use no args), (2) specify just the --port, and (3) specify all args")
  (println "except maybe keep the default port (8855)."))
           
(defn -main [& args]
  (binding [*warn-on-reflection* false] ; Reflection in org.apache.poi.openxml4j.util.ZipSecureFile$1
    (let [pairs (partition 2 args)
          switches (map first pairs)]
      (if (and (every? valid-arg? switches)
               (even? (count args)))
        (apply core/start-server! (reduce (fn [result [sw v]]
                                            (as-> result ?r
                                                (conj ?r (arg2key sw))
                                                (if (= sw "--port")
                                                  (conj ?r (read-string v))
                                                  (conj ?r v))))
                                          []
                                          pairs))
        (usage)))))



