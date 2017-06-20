(ns muchjs.core
  (:refer-clojure :exclude [])
  (:require [cljs.nodejs :as nodejs]
            [fipp.edn :refer [pprint]]
            [cljs.pprint]
            )
  (:use [regexpforobj.core :only [InputChar
                                  Char Or Seq Star
                                  grammar_pretty run is_parsing_error?
                                  ]])
  )


(def ^:dynamic *display-function-forms* false)


(defonce last-source (atom nil))

(nodejs/enable-util-print!)

(defn iprint [x]
  (println)
  (prn x)
  (println)
  x
  )

(def fs (nodejs/require "fs"))
(def babylon (nodejs/require "babylon"))


(defn opt1 [x]
  (if (and (list? x)
           (sequential? x)
           (sequential? (first x))
           (symbol? (ffirst x))
           (= (subs (name (ffirst x)) 0 2) ".-")
           )
    (->>
      (rest x)
      (cons (second (first x)))
      (cons (symbol (str "." (subs (name (ffirst x)) 2))))
      )
    x)
  )

(declare transform1)


(defn fnbody1 [body]
  (let [input (map
         (fn [element] (InputChar (:type element)
                     (clojure.walk/postwalk #(cond-> %
                                                       (map? %)
                                                       (dissoc :loc
                                                               :start
                                                               :end)
                                                       ) element)
                     ))
         (:body body))
        g (Seq
            [
             (Star (Char "VariableDeclaration"))
             (Char "ReturnStatement"
                  (fn [x]
                    (transform1 (:payload (:value x)))
                    )
                  )
             ]
              (fn [x]
                (let [[exprs ret] (:value x)

                      ret (transform1 (:payload ret))
                      
                      exprs (map (comp
                                   #(get-in % [:declarations 0])
                                   :payload)
                                 (:value exprs))
                  exprs (vec (mapcat
                               (fn [e]
                                 [(transform1 (:id e)) (transform1 (:init e))]
                                 )
                               exprs
                               ))
                      ]
                      (if (empty? exprs) ret (list 'let exprs ret))
                  
                  )
                )
            )
            

        r (run g input) ; TODO check for errors (is_parsing_error?)
        r2 (clojure.walk/postwalk
             (fn [x]
               (cond-> x
                 (and (map? x) (or (fn? (:payload x)) (keyword? (:payload x))))
                 ((:payload x) (:value x))
                 )
               )
             r
             )
        ]
    (when *display-function-forms*
  (println "**********************")
  (cljs.pprint/pprint (-> input
           
           grammar_pretty
           ))
  (println "----------------------")
      )

    (list r2)
    )
  )

(defn drop-loc-info [tree]
  (clojure.walk/postwalk
    (fn [node]
      (if (map? node)
        (dissoc node :loc :start :end)
        node)
      )
    tree)
  )

(defn transform1 [{:keys [type] :as obj}]
  (cond
    (and (= type "Program") (= (:sourceType obj) "module"))
    (map transform1 (:body obj))

    (and (= type "VariableDeclaration") (:kind obj) "var")
    (mapcat transform1 (:declarations obj))

    (and (= type "VariableDeclarator") (= (:type (:id obj)) "Identifier"))
    (list 'def (symbol (:name (:id obj))) (transform1 (:init obj)))

    (and (= type "CallExpression"))
    (opt1 (cons (transform1 (:callee obj)) (map transform1 (:arguments obj))))

    (and (= type "Literal"))
    (:value obj)

    (and (= type "StringLiteral"))
    (:value obj)

    (and (= type "NumericLiteral"))
    (:value obj)

    (and (= type "IdentifierString"))
    (:name obj)

    (and (= type "MemberExpression") (= (:type (:property obj)) "Identifier"))
    (list (symbol (str ".-" (:name (:property obj)))) (transform1 (:object obj)))

    (and (= type "Identifier"))
    (symbol (:name obj))

    (and (= type "ArrayExpression"))
    (cons 'clj->js [ (mapv identity
          (map transform1 
          (:elements obj)
               )
          )])

    (and (= type "ObjectExpression"))
    (apply js-obj (map identity
          (mapcat (fn [nobj]
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
          (:properties obj)
               )
          ))

    (and (= type "FunctionExpression") (= (:type (:body obj)) "BlockStatement"))
    (cons 'fn (cons (mapv transform1 (:params obj))
                    (list (list 'this-as 'this 
          (first (fnbody1 (:body obj)))))
          ))

    (and (= type "ArrowFunctionExpression"))
    (let [params
          (into []
                (comp
                  (map-indexed list)
                  (map
                    (fn [[i param]]
                      (cond
                        (= (:type param) "ObjectPattern")
                        (let [its-symbol (symbol (str "__obj" i))]
                          {:param its-symbol
                           :let (map
                                  (fn [p]
                                    [(transform1 (:key p))
                                     (list
                                       (symbol
                                         (str
                                           ".-"
                                           (name (transform1 (:value p)))))
                                       its-symbol
                                       )])
                                  (:properties param))})
                        :else
                        {:param (transform1 param)}))))
                (:params obj))]
      (cons 'fn (cons (mapv :param params)

                      (list (list 'let (vec (mapcat identity (mapcat :let params)))
            (first (fnbody1 (:body obj))))))))

    (and (= type "BlockStatement"))
    (map transform1 (:body obj))

    (and (= type "ReturnStatement")) ; TODO: check that return statement is in fact last
    (transform1 (:argument obj))

    (and (= type "FunctionDeclaration") (= (:type (:body obj)) "BlockStatement") (= (:type (:id obj)) "Identifier"))
    (cons 'defn (cons (transform1 (:id obj)) (cons (mapv transform1 (:params obj))
          (fnbody1 (:body obj))
          )))

    (and (= type "ExpressionStatement"))
    (transform1 (:expression obj))

    (and (= type "AssignmentExpression"))
    (list 'set!
          (transform1 (:left obj))
          (transform1 (:right obj))
          )

    (and (= type "LogicalExpression"))
    (list (get {"&&" 'and "||" 'or} (:operator obj))
          (transform1 (:left obj))
          (transform1 (:right obj)))

    (and (= type "SequenceExpression"))
    (apply
      list 
          (map transform1 (:expressions obj)))

    (and (= type "ThisExpression"))
    (do
      'this
      )
    (or (and (= type "NullLiteral")) (nil? obj))
    nil

    (and (= type "JSXElement"))
    (vec (->>
      (map
        transform1
        (:children obj)
        )
      (cons
        (into {}
          (map (fn [a]
                 [(keyword (transform1 (:name a))) (transform1 (:value a))]
                 )
               (-> obj :openingElement :attributes)
               )
          )
        )
      (cons (keyword (get-in obj [:openingElement :name :name])))
      ))

    (and (= type "JSXText"))
    (:value obj)

    (and (= type "JSXExpressionContainer"))
    (transform1 (:expression obj))

    (and (= type "JSXIdentifier"))
    (:name obj)

    (and (= type "ConditionalExpression"))
    (list 'if (transform1 (:test obj))
              (transform1 (:consequent obj))
              (transform1 (:alternate obj))
          )

    #_(and (= type "ImportDeclaration"))
    #_(list 'if (transform1 (:test obj))
              (transform1 (:consequent obj))
              (transform1 (:alternate obj))
          )

    :else
    (do
      (cljs.pprint/pprint
        (drop-loc-info
         obj))
      (throw (js/Error. (str "Unsupported type: " (pr-str type))))
      )
    )
  )

(defn transform2 [obj]
  (transform1 obj)
  )

(defn convert [source]
  (binding [cljs.pprint/*print-right-margin* 120]
(let
  [
   parsed (.parse babylon source (clj->js {:plugins ["jsx" "flow" "doExpressions" "objectRestSpread" "decorators" "classProperties" "exportExtensions" "asyncGenerators" "functionBind" "functionSent"]
                                           :sourceType "module" ; TODO guess
                                           }))
   data (js->clj (js/JSON.parse (js/JSON.stringify parsed)) :keywordize-keys true)
   ;data (update-in data [:body] #(take 6 %))
   ]
  ;(println)
  ;(println)
  ;(prn data)
  (println)
  (println)
  (try
    (doseq [expr (transform2 (:program data))]
      (cljs.pprint/pprint expr)
      (println)
      )
    (catch js/Error e
      (println "Error" e)
      )
    )
  (println)
  (println)
  ;(prn (sort (apply hash-set (map second (filter #(and (sequential? %) (= :type (first %))) (tree-seq #(or (map? %) (sequential? %)) identity data))))))
  #_(prn (into []
             (comp
               (filter #(= (:type %) "VariableDeclaration"))
               (map #(list 'def (symbol (-> % :declarations first :id :name)) "foo"))
               )
             (:body data)
             ))
  )))

(when-let [source
      @last-source
      ]
  (convert source)
  )

(defn -main [filename]
  ; ...

   (let [source (str (.readFileSync fs filename))]
     (reset! last-source source)
     (convert source)
     )
  )

(set! *main-cli-fn* -main)


