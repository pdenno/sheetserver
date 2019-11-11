(ns pdenno.sheetserver.util
  (:require
   [clojure.string    :as string]
   [clojure.pprint    :refer (pprint cl-format)]
   [clojure.walk      :as walk]
   [clojure.data.json :as json]))

(defn kw-keys 
  "In map m, replace string keywords with its keyword."
  [m]
  (reduce
   (fn [mm k]
        (if (string? k)
          (-> mm
              (assoc (keyword k) (get m k))
              (dissoc k))
          mm))
   m
   (keys m)))

;;; POD there is a standard way to do this. See pydict2map below.
(defn keywordize
  "Recursively update object, changing keys from strings to keywords."
  [m]
  (cond (map? m) (as-> m ?m
                   (kw-keys ?m)
                   (reduce (fn [mm k] (update mm k keywordize)) ?m (keys ?m)))
        (vector? m) (mapv keywordize m)
        :else m))

(defn read-json
  "Return a clojure-ized object from reading a JSON file."
  [filename]
  (-> filename
      slurp 
      json/read-str
      keywordize))

;;; These can be deleted once I've check the slacker solution below. 
#_(defn convert-to-base
  "Return a string representing the argument number in a new arbitrary base (up to 36, I hear)."
  [n to-base]
  (when (> to-base 36) (throw (ex-info "Max radix is 36" {:radix n})))
  (. Integer toString n to-base))

#_(defn column-key
  "Return the column name corresponding to n (n=1 --> :A, n=28 --> :AC)"
  [n]
  (when (> n 676) (throw (ex-info "Column-letters n>676" {:n n})))
  (let [letters [\A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T \U \V \W \X \Y \Z]]
    (if (< n 27)
      (->> n dec (nth letters) str keyword)
      (let [right (->> (mod n 26) dec (nth letters))
            left  (->> (->  n (/ 26) Math/floor int) dec (nth letters))]
        (keyword (str left right))))))

;;; This, from a slacker, to replace the previous two functions!
(defn string-permute
  ([chars] (string-permute [""] chars))
  ([prev chars]
   (let [strs (mapcat (fn [c] (map (fn [s] (str c s)) prev)) chars)]
     (lazy-cat strs (string-permute strs chars)))))

(defn now [] (new java.util.Date))
