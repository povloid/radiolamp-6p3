(ns r6p3.complex.areal-core
  (:use r6p3.core)
  (:require [korma.db :as kdb]
            [korma.core :as kc]))



;;;hydra-areal>>

;;;**************************************************************************************************
;;;* BEGIN areal
;;;* tag: <ref book core layer areal>
;;;*
;;;* description: Сервисный слой для Области, районы...
;;;*
;;;**************************************************************************************************

(declare areal-preparation)

(kc/defentity areal-childs
  (kc/pk :id)
  (kc/table "areal")
  (kc/transform (fn [row]
                  (-> row
                      (dissoc :fts :fts_up)))))


(declare areal)

(kc/defentity areal
  (kc/pk :id)

  (kc/prepare (fn [row]
                (-> row
                    )))

  (kc/transform (fn [row]
                  (-> row
                      (dissoc :fts :fts_up))))

  (kc/belongs-to areal {:fk :parent_id})
  (kc/has-many areal-childs {:fk :parent_id}))



(defn areal-save [row]
  (kdb/transaction
   (let [;; Сначала сохраняем
         {:keys [id parent_id] :as row-2} (com-save-for-id areal row)
         ;; получаем реальное имя таблици в базе данных для raw запросов
         table                            (areal :table)
         ;; Сначала поднимаем родителя
         parents                          (pg--raw--select-tree-parents {:id parent_id} table)

         ;; сразу проверим правильность присвоения родителя, нет ли в родителях этого же элемента
         ;; Очень важная защита от размыкания и циклического замыкания дерева
         _ (when (some #(-> % :id (= id)) parents)
             (throw (Exception. (str "Попытка вставить элемент в самого себя !"))))

         parents (reverse (conj parents row-2)) ;; тут уже формируем путь

         path-ids      (->> parents
                            (map (comp #(str % " ") :id))
                            (reduce str ""))
         path-keynames (->> parents
                            (map (comp #(str " / " %) :keyname))
                            (reduce str ""))

         row-3 (com-save-for-id areal {:id id :path_ids path-ids :path_keynames path-keynames})]

     (->> (-> areal
              kc/select*
              (kc/where (= :parent_id id))
              kc/exec)
          (map areal-save)
          doall)
     row-3)))


(defn areal--pg--raw--select-tree-sub-childs
  "Выбор дочерних элементов напрямую из базы как есть"
  [row]
  (pg--raw--select-tree-sub-childs row (areal :table)))




(def areal-select* (kc/select* areal))

(defn areal-pred-search? [areal-select* fts-query]
  (com-pred-full-text-search* areal-select* :fts fts-query))


;;; END areal
;;;..................................................................................................

;;;<<hydra-areal
