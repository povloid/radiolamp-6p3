(ns r6p3.dataform
  (:use clojure.pprint)
  (:require [korma.db :as kdb]
            [korma.core :as kc]
            [cheshire.core :as cheshire-c]))


;;;**************************************************************************************************
;;;* BEGIN define
;;;* tag: <dataform define>
;;;*
;;;* description: Иницилизатор
;;;*
;;;**************************************************************************************************

(defn- make-sql-raw [s]
  (print "MAKE SQL: " s " ")
  (try
    (kc/exec-raw s)
    (catch Exception ex
      (do (print " [EX]")
          (println (.getMessage ex)))))
  (println " [OK]"))



(defmulti make-sql
  (fn [_ field-k {:keys [type] :as params}]
    (println "----------------------------------------------------")
    (println "ОБРАБОТКА: " field-k)
    (pprint params)
    type))


(defmethod make-sql :default
  [_ field-k {:keys [type] :as params}]
  (println "ВНИМАНИЕ: " field-k " ->  непонятный тип: " type)
  (println "HE ОБРАБОТАН!"))

(defmethod make-sql :string
  [{:keys [entity-main]} field-k _]
  (make-sql-raw
   (str
    "ALTER TABLE " (:table entity-main)
    " ADD COLUMN " (name field-k)
    " character varying(50);")))

(defmethod make-sql :text
  [{:keys [entity-main]} field-k _]
  (make-sql-raw
   (str
    "ALTER TABLE " (:table entity-main)
    " ADD COLUMN " (name field-k)
    " text;")))

(defmethod make-sql :boolean
  [{:keys [entity-main]} field-k _]
  (make-sql-raw
   (str
    "ALTER TABLE " (:table entity-main)
    " ADD COLUMN " (name field-k)
    " boolean;")))

(defmethod make-sql :integer
  [{:keys [entity-main]} field-k _]
  (make-sql-raw
   (str
    "ALTER TABLE " (:table entity-main)
    " ADD COLUMN " (name field-k)
    " integer;")))

(defmethod make-sql :money
  [{:keys [entity-main]} field-k _]
  (make-sql-raw
   (str
    "ALTER TABLE " (:table entity-main)
    " ADD COLUMN " (name field-k)
    " numeric(14,2);")))

(defmethod make-sql :rbs
  [{:keys [entity-main entitys]} field-k {:keys [rbentity]}]
  (let [field-k         (name field-k)
        entity-table    (:table entity-main)
        entity-name     (:name entity-main)
        rb-entity-table (:table (entitys rbentity))
        rb-entity-name  (:name (entitys rbentity))]
    (make-sql-raw
     (str
      "ALTER TABLE " entity-table
      " ADD COLUMN " field-k
      " integer"))
    (make-sql-raw
     (str
      "ALTER TABLE " entity-table
      " ADD CONSTRAINT " entity-name "__" rb-entity-name "__" field-k "__fk"
      " FOREIGN KEY (" field-k ")"
      " REFERENCES " rb-entity-table " (id) MATCH SIMPLE"
      " ON UPDATE NO ACTION ON DELETE NO ACTION;"))))



(defn scheme-init [{{:keys [entity-main entitys]
                     :as   main-map}                :main-map
                    {:keys [realtype-field fields]} :rbs-scheme}]
  (make-sql-raw
   (str
    "ALTER TABLE " (:table entity-main)
    " ADD COLUMN " (name realtype-field)
    " character varying(50);"))
  
  (map
   (fn [[field-k params]]
     (make-sql main-map field-k params))
   (seq fields)))

;;; END define
;;;..................................................................................................

;;;**************************************************************************************************
;;;* BEGIN Korma tools
;;;* tag: <korma tools>
;;;*
;;;* description: Функционал для формирования запросов
;;;*
;;;**************************************************************************************************

(defn rbs-data [{{:keys [entitys]} :main-map
                 {:keys [fields]}  :rbs-sheme}]
  
  (if (empty? entitys) {} ;; если еще не проиниц. пропускаем
    (->> entitys seq
         (reduce
          (fn [a [k ent]]
            (->> (kc/select ent
                            (kc/fields :id :keyname :rbtype)
                            (kc/where (not (= :rbtype nil))))                           
                 (group-by :rbtype)
                 (reduce-kv
                  (fn [a k v]
                    (assoc a k (map #(dissoc % :rbtype) v)))
                  {})
                 (assoc a k)))
          {}))))


;;; END Korma tools
;;;..................................................................................................

;;;**************************************************************************************************
;;;* BEGIN base functional
;;;* tag: <dataform base functional>
;;;*
;;;* description: Базовый функционал для датаформы
;;;*
;;;**************************************************************************************************


(def dataform-k
  :dataform)

(def dataform-rb-ks
  [dataform-k :rb])


(def app-init
  (-> {dataform-k {}}
      (assoc-in dataform-rb-ks {})))


(defn add-rb [data {:keys [rb-k list-fn]}]
  (let [rb-ks (conj dataform-rb-ks rb-k)]
    (if (get-in data rb-ks)
      data
      (assoc-in data rb-ks (or (list-fn data) (list))))))






(defmulti transform (fn [_ {:keys [type]}] type))

(defn transform-row [data dataform]
  (reduce transform (assoc app-init :row data) dataform))


(defmulti prepare (fn [_ {:keys [type]}] type))

(defn prepare-row [data dataform]
  (reduce prepare (assoc app-init :row data) dataform))

;;; END base functional
;;;..................................................................................................


;;------------------------------------------------------------------------------
;; BEGIN: default behavior
;; tag: <dataform default behavior>
;; description: По умолчанию
;;------------------------------------------------------------------------------

(defmethod transform :default
  [data _]
  data)

(defmethod prepare :default
  [data _]
  data)

;; END default behavior
;;..............................................................................


(defmethod transform :string
  [data _]
  data)
(defmethod prepare :string
  [data _]
  data)


(defmethod transform :number
  [data _]
  data)
(defmethod prepare :number
  [data _]
  data)


(defmethod transform :select
  [data params]
  (add-rb data params))
(defmethod prepare :select
  [data _]
  data)
