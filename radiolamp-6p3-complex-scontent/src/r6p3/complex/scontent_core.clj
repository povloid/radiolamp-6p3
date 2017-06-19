(ns r6p3.complex.scontent-core
  (:use r6p3.core)
  (:require [korma.db :as kdb]
            [korma.core :as kc]))

;;;hydra-scontent>>

;;;**************************************************************************************************
;;;* BEGIN scontent
;;;* tag: <ref book core layer scontent>
;;;*
;;;* description: Сервисный слой для контент для сайта
;;;*
;;;**************************************************************************************************

(kc/defentity scontent
  (kc/pk :id)

  (kc/prepare (fn [row]
                (-> row
                    ((partial prepare-as-string :keyname))
                    ((partial prepare-as-string :vtype)))))

  (kc/transform (fn [row]
                  (-> row
                      ((partial transform-as-keyword :keyname))
                      ((partial transform-as-keyword :vtype))
                      (dissoc :fts)))))

(defn scontent-save [row]
  (com-save-for-id scontent (dissoc row :keyname :vtype)))

(defn scontent-get-v [vtype keyname]
  (-> scontent
      kc/select*
      (kc/fields [(str "v_" (name vtype)) :v])
      (kc/where (and (:vtype (name vtype) (= :keyname (name keyname)))))
      com-exec-1))

(defn scontent-get [vtype keyname]
  (:v (scontent-get-v vtype keyname)))


(def scontent-select* (kc/select* scontent))

(defn scontent-pred-search? [scontent-select* fts-query]
  (com-pred-full-text-search* scontent-select* :fts fts-query))


(defn scontent-def [row]
  (com-save-for-field scontent :keyname (update-in row [:keyname] name)))

;;; END scontent
;;;..................................................................................................

;;;<<hydra-scontent



