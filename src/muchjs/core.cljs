(ns muchjs.core
  (:refer-clojure :exclude [])
  (:require [cljs.nodejs :as nodejs]
            [fipp.edn :refer [pprint]]
            )
  ;(:use [aprint.core :only [aprint]])
  )


(nodejs/enable-util-print!)

(def fs (nodejs/require "fs"))
(def esprima (nodejs/require "esprima"))

(defn transform1 [{:keys [type] :as obj}]
  (cond
    (and (= type "Program") (:sourceType obj) "script")
    (map transform1 (:body obj))

    (and (= type "VariableDeclaration") (:kind obj) "var")
    (map transform1 (:declarations obj))

    (and (= type "VariableDeclarator") (= (:type (:id obj)) "Identifier"))
    (list 'def (symbol (:name (:id obj))) (transform1 (:init obj)))

    (and (= type "CallExpression") (= (:type (:callee obj)) "Identifier"))
    (cons (symbol (:name (:callee obj))) (map transform1 (:arguments obj)))

    (and (= type "Literal"))
    (:value obj)

    (and (= type "MemberExpression") (= (:type (:property obj)) "Identifier"))
    (list (symbol (str ".-" (:name (:property obj)))) (transform1 (:object obj)))

    :else
    (throw (js/Error. (str "Unsupported type " type)))
    )
  )

(let
  [
   source (str (.readFileSync fs "/home/andrey/example2.js"))
   parsed (.parse esprima source)
   data (js->clj (js/JSON.parse (js/JSON.stringify parsed)) :keywordize-keys true)
   data (update-in data [:body] #(take 3 %))
   ]
  (prn data)
  (try
    (prn (transform1 data))
    (catch js/Error e
      (println "Error" e)
      )
    )
  (prn (sort (apply hash-set (map second (filter #(and (sequential? %) (= :type (first %))) (tree-seq #(or (map? %) (sequential? %)) identity data))))))
  #_(prn (into []
             (comp
               (filter #(= (:type %) "VariableDeclaration"))
               (map #(list 'def (symbol (-> % :declarations first :id :name)) "foo"))
               )
             (:body data)
             ))
  )

(defn -main []
  (println "Hello Peter!!"))

(set! *main-cli-fn* -main)
