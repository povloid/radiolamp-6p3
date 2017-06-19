(ns r6p3.complex.page-core
  (:use r6p3.core)
  (:require [korma.db :as kdb]
            [korma.core :as kc]))



;;;**************************************************************************************************
;;;* BEGIN Content Type
;;;* tag: <content type>
;;;*
;;;* description: Тип контента
;;;*
;;;**************************************************************************************************

(kc/defentity content_type
  (kc/pk :id)
  (kc/prepare (fn [row]
                (-> row
                    ((partial prepare-as-string :keyname)))))

  (kc/transform (fn [row]
                  (-> row
                      ((partial transform-as-keyword :keyname))))))

(defn content_type-init [meta-data]
  (->> meta-data
       :content-type
       (map (fn [[k row]]
              (com-save-for-field
               content_type :keyname
               (assoc row :keyname (name k)))))
       doall))

;;; END Content Type
;;;..................................................................................................

;;;hydra-page>>

;;;**************************************************************************************************
;;;* BEGIN page
;;;* tag: <ref book core layer page>
;;;*
;;;* description: Сервисный слой для страница
;;;*
;;;**************************************************************************************************


(kc/defentity page
  (kc/pk :id)

  (kc/prepare (fn [row]
                (-> row
                    )))

  (kc/transform (fn [row]
                  (-> row
                      (dissoc :fts)
                      )))

  (kc/many-to-many content_type :page_content_type))

(kc/defentity page_content_type
  (kc/pk :id))

(defn page-save [{:keys [content-types] :as row}]
  (kdb/transaction
   (let [{page-id :id :as row} (com-save-for-id page (dissoc row :content-types))]

     (when content-types
       (kc/delete page_content_type (kc/where (= :page_id page-id)))
       (doseq [{id :id} (kc/select content_type (kc/fields :id) (kc/where (in :keyname (map name content-types))))]
         (kc/insert page_content_type (kc/values {:page_id page-id :content_type_id id}))))

     row)))


(def page-select* (kc/select* page))

(defn page-pred-search? [page-select* fts-query]
  (com-pred-full-text-search* page-select* :fts fts-query))


;;; END page
;;;..................................................................................................

;;;<<hydra-page


