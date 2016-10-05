(ns muchjs.core
  (:refer-clojure :exclude [])
  (:require [cljs.nodejs :as nodejs]
            [fipp.edn :refer [pprint]]
            )
  ;(:use [aprint.core :only [aprint]])
  )


(nodejs/enable-util-print!)

(defn iprint [x]
  (println)
  (prn x)
  (println)
  x
  )

(def fs (nodejs/require "fs"))
(def esprima (nodejs/require "esprima"))

(defn transform1 [{:keys [type] :as obj}]
  (cond
    (and (= type "Program") (:sourceType obj) "script")
    (mapcat transform1 (:body obj))

    (and (= type "VariableDeclaration") (:kind obj) "var")
    (map transform1 (:declarations obj))

    (and (= type "VariableDeclarator") (= (:type (:id obj)) "Identifier"))
    (list 'def (symbol (:name (:id obj))) (transform1 (:init obj)))

    (and (= type "CallExpression") (= (:type (:callee obj)) "Identifier"))
    (cons (symbol (:name (:callee obj))) (map transform1 (:arguments obj)))

    (and (= type "Literal"))
    (:value obj)

    (and (= type "IdentifierString"))
    (:name obj)

    (and (= type "MemberExpression") (= (:type (:property obj)) "Identifier"))
    (list (symbol (str ".-" (:name (:property obj)))) (transform1 (:object obj)))

    (and (= type "Identifier"))
    (symbol (:name obj))

    (and (= type "ObjectExpression"))
    (into {}
          (map (fn [nobj]
                 (let [k (cond->
                                    (:key nobj)

                                    (= (:type (:key nobj)) "Identifier")
                                    (assoc :type "IdentifierString")
                                    )
                       v (:value nobj)

                       k (transform1 k)
                       v (transform1 v)
                       ]
                   [k v]
                   )
                 )
               )
          (:properties obj)
          )

    (and (= type "FunctionExpression") (= (:type (:body obj)) "BlockStatement"))
    (cons 'fn (cons (mapv transform1 (:params obj))
          (transform1 (:body obj))
          ))

    (and (= type "BlockStatement"))
    (map transform1 (:body obj))

    (and (= type "ReturnStatement")) ; TODO: "check last"
    (transform1 (:argument obj))

    :else
    (throw (js/Error. (str "Unsupported type " type)))
    )
  )

(let
  [
   source (str (.readFileSync fs "/home/andrey/example2.js"))
   parsed (.parse esprima source)
   data (js->clj (js/JSON.parse (js/JSON.stringify parsed)) :keywordize-keys true)
   data (update-in data [:body] #(take 5 %))
   ]
  (println)
  (println)
  (try
    (doseq [expr (transform1 data)]
      (prn expr)
      (println)
      )
    (catch js/Error e
      (println "Error" e)
      )
    )
  (println)
  (println)
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
