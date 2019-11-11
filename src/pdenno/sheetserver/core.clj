(ns pdenno.sheetserver.core
  "Implement an Open API-based server that returns data from an .xslx (maybe more) spreadsheet."
  (:require
   [clojure.tools.logging        :as log]
   [clojure.pprint :refer (pprint cl-format)]
   [clojure.string               :as str]
   [dk.ative.docjure.spreadsheet :as ss]
   [org.httpkit.server           :as hk]
   [reitit.ring                  :as ring]
   [pdenno.sheetserver.util      :as util]))

;;; CURRENT LIMITATIONS:
;;;  (1) Only handles a simple functional GET with a one-cell primary key.
;;;  (2) Sends back data as a string body of the response.
;;; TODO:
;;;  Use reitit swagger stuff for spec. 

(def diag "Used for debugging." (atom nil))
(defonce server (atom nil))

(defn handler-for-dispatch
  "This chooses the method by searching for one tagged with a vector of the first two args."
  [access-type http-method path]
  (vector access-type http-method))

(defmulti handler-for #'handler-for-dispatch)

(declare get-qparam)
(defmethod handler-for [:at-most-one :get]
  ;; Using the mapping info, return a function that uses a single parameter to access the data requested.
  [_ _ map-key]
  (fn [req]
    (if-let [mapping (some #(when (= map-key (:path %)) %) (:mapping @server))]
      (if-let [qkey (-> mapping :map :key :query-param)]
        (if-let [qparam (get-qparam req qkey)]
          (if-let [key-fn  (-> mapping :map :key :column)]
            (if-let [val-fn  (-> mapping :map :value :column)]
              (if-let [val (val-fn (some #(when (= (key-fn %) qparam) %) (:spreadsheet @server)))]
                {:status 200 :body (str val)}
                {:status 404 :body (str "No resource at " qparam)})
              {:status 404 :body (str "Must specify a value column in mapping JSON.")}) ; ahem! pretty!
            {:status 404 :body (str "Must specify a key column specified in mapping JSON.")})
          {:status 404 :body (str "The query parameter " qkey " was not in the URI.")})
        {:status 404 :body (str "Must specify a query-param in the mapping JSON.")})
      {:status 404 :body (str "Must specify a mapping (in the JSON file) for the URI.")})))

(defn get-qparam
  "q-param is a string naming a parameter on the URI query string, return its value from the request, req."
  [req q-param]
  (second (re-matches (re-pattern (cl-format nil ".*~A=(.*)" q-param))
                      (:query-string req))))

(defn endpoint-ok
  "A response for testing"
  [req]
  {:status 200, :body (str "Endpoint: I'm okay " (util/now))})

(defn data-ok? [k m]
  (let [known-data? {:type #{:at-most-one} :method #{:get}}]
    (if-let [val (k m)]
      (if ((k known-data?) val)
        val
        (throw (ex-info (str val " is not a valid value for " k ".") {:map m :val val})))
      (throw (ex-info (str "Specify a " (name k) " in the mapping JSON.") {:map m})))))

;;; We break this out for diagnostics. 
(defn make-handler-vecs
  "Make the reitit handler vectors."
  [maps]
  (conj (mapv (fn [m]
                (let [type   (data-ok? :type   m)
                      method (data-ok? :method m)]
                  (if-let [path (:path m)]
                    (vector path {method (handler-for type method path)})
                    (throw (ex-info "Specify a path in the mapping" {:map m})))))
              maps)
        ["/sheetserver/ok" {:get endpoint-ok :name ::ok}]))

;;; Example: http://localhost:8855/warehouse/matl-onhand?item-id=BIND-ARAM
;;; Example: (app {:request-method :get, :uri "/warehouse/matl-onhand?item-id=BIND-ARAM"})
(defn make-handlers
  "From the Open API spec, create a reitit- and ring-based http router."
  [maps]
  (ring/ring-handler
   (ring/router (make-handler-vecs maps))
   ;; the default handler
   (ring/create-default-handler)))

(defn read-spreadsheet
  "Read the .xlsx and make a clojure map for each row. No fancy names, just :A,:B,:C,...!"
  [filename sheet-name]
  (when-let [sheet (->> (ss/load-workbook filename) (ss/select-sheet sheet-name))]
    (let [row1 (mapv ss/read-cell (-> sheet ss/row-seq first ss/cell-seq ss/into-seq))
          len  (loop [n (dec (-> sheet ss/row-seq first .getLastCellNum))]
                 (cond (= n 0) 0, 
                       (not (nth row1 n)) (recur (dec n))
                       :else (inc n)))
          keys (map keyword (take len (util/string-permute "ABCDEFGHIJKLMNOPQRSTUVWXYZ")))]
               #_(mapv util/column-key (range 1 (inc len)))
      ;; POD This is all sort of silly. Can we access cells a better way?
      (ss/select-columns (zipmap keys keys) sheet))))

(defn cleanup-map
  "The JSON from the user is less than optimal."
  [jmaps]
  (mapv
   #(-> % ; jmaps is a vector of maps, one for each path + http-method
        (update :method keyword)
        (update :type   keyword)
        (update-in [:map :key   :column]  keyword)
        (update-in [:map :value :column]  keyword))
   jmaps))

(defn start-server!
  "Create a server (a map) from Open API JSON, spreadsheet data and user map; start it." 
  [& ; Default args run an example found in the resources directory.
   {:keys [port mapping schema spreadsheet sheet]
    :or   {port        8855
           mapping     "resources/sheet2uri-map.json"
           schema      "resources/warehouse.json"
           spreadsheet "resources/on-hand.xlsx"
           sheet       "on-hand"}}]
  (when (nil? @server)
    (log/info "Server starting on port" port)
    (as-> {} ?s
      (assoc ?s :mapping (-> (util/read-json mapping) cleanup-map))
      (assoc ?s :open-api (util/read-json schema))
      (assoc ?s :spreadsheet (read-spreadsheet spreadsheet sheet))
      (assoc ?s :server (make-handlers (:mapping ?s)))
      (assoc ?s :instance (hk/run-server (:server ?s) {:port port}))
      (reset! server ?s)
      true)))

(defn stop-server!
  "Stop the server (to reload it in development)."
  []
  (when-not (nil? @server)
    (log/info "Stopping sheetserver")
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    ((:instance @server) :timeout 100)
    (reset! server nil)))
