(ns ixinfestor.core
  
  (:use korma.db)
  (:use korma.core)
  (:use clojure.pprint)

  (:require [clj-time.core :as tco]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clj-time.local :as tl])
  )


;;**************************************************************************************************
;;* BEGIN SQL TOOLS
;;* tag: <sql tools>
;;*
;;* description: Sql tools fns
;;*
;;**************************************************************************************************


;; COMMON FUNCTIONS ----------------------------------------------------
(defn com-save-for-field
  "Сохранить сущность"
  [entity field vals]
  (let [r (update entity (set-fields vals) (where (= field (vals field))))]
    (if (empty? r)
      (insert entity (values vals))
      r)))

(defn com-save-for-field-2
  "Сохранить сущность"
  [entity field-1 field-2 vals]
  (let [r (update entity (set-fields vals) (where (and
                                                   (= field-1 (vals field-1))
                                                   (= field-2 (vals field-2))
                                                   )))]
    (if (empty? r)
      (insert entity (values vals))
      r)))

(defn com-save-for-id
  "Сохранить сущность"
  [entity vals]
  (let [r (update entity (set-fields vals) (where (= :id (vals :id))))]
    (if (empty? r)
      (insert entity (values vals))
      r)))

(defn com-delete*
  "Удалить сущность"
  [entity]
  (delete* entity))

(defn com-delete-for-id
  "Удалить сущность по id"
  [entity id]
  (delete entity (where (= :id id))))

(defn com-find
  "Найти сущность по :id"
  [entity id]
  (first (select entity (where (= :id id)))) )

(defn com-count
  "Количество всех элементов"
  [entity]
  (-> (select entity (aggregate (count :*) :c)) first :c)  )

(defn com-exec
  "Выполнить запрос"
  [query]
  (exec query))

(defn com-pred-page* [query* page size]
  (-> query* (limit size) (offset (* page size))))

(defn com-pred-full-text-search* [query* fts-field fts-query]
  (let [fts-query (->> (clojure.string/split (str fts-query) #"\s+")
                       (map #(str % ":*"))
                       (reduce #(str %1 " & " %2)))]
    (where query* (raw (str " " (name fts-field) " @@ to_tsquery('" fts-query "')")))))

(defn com-defn-add-rel-many-to-many
  "создание функции соединения двух сущностей по типу many-to-many"
  [field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-1-id field-id-1} {entity-2-id field-id-2}]
    (let [entity-2-row (select rel-entity
                               (where (and
                                       (=  field-fk-id-1 entity-1-id)
                                       (=  field-fk-id-2 entity-2-id))))]
      (if (empty? entity-2-row) (insert rel-entity
                                        (values {field-fk-id-1 entity-1-id field-fk-id-2 entity-2-id}))
          (first entity-2-row)))))



(defn com-defn-has-a-rel?
  "создание функции проверки наличия соединения двух сущностей по типу many-to-many"
  [entity-2 field-id-2 field-keyname-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-1-id field-id-2} entity-2-keyname]
    (transaction
     (let [entity-2s-ids (select entity-2
                                 (fields field-id-2)
                                 (where (= field-keyname-2 (name entity-2-keyname))))]
       (if (empty? entity-2s-ids)
         (throw (Exception. (str "Row with " field-keyname-2 " = " entity-2-keyname " is not described.")))
         (not (empty? (select rel-entity
                              (where (and
                                      (= field-fk-id-1 entity-1-id)
                                      (= field-fk-id-2 (-> entity-2s-ids first field-id-2))))))))))))

(defn com-defn-get-rels-set
  [entity-2 field-id-2 field-keyname-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-1-id field-id-2}]
    (transaction
     (->> (select entity-2 (fields field-keyname-2)
                  (where {field-id-2 [in (subselect rel-entity
                                                    (fields field-fk-id-2)
                                                    (where (= field-fk-id-1 entity-1-id)))]}))
          (map field-keyname-2)
          set))))



(defn com-defn-get-rows-by-rel*
  [entity-1 field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-2-id field-id-2}]
    (-> (select* entity-1)
        (where {field-id-1 [in (subselect rel-entity
                                          (fields field-fk-id-1)
                                          (where (= field-fk-id-2 entity-2-id)))]}))))

(defn com-defn-get-rows-by-rel--nil-other*
  [entity-1 field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-2-id field-id-2 :as row}]
    (if (nil? entity-2-id)
      ;; nil
      (-> (select* entity-1)
          (where (not (exists
                       (subselect rel-entity
                                  (where (= field-fk-id-1
                                            (-> entity-1
                                                :table
                                                str
                                                (str "." (name field-id-1))
                                                keyword)
                                            )))))))
      ;; not nil
      ((com-defn-get-rows-by-rel* entity-1 field-id-1 field-id-2
                                  rel-entity field-fk-id-1 field-fk-id-2) row ))))



;; END SQL TOOLS
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN Entitys utilites
;;* tag: <entitys utils>
;;*
;;* description: Вспомогательные утилиты
;;* TODO: НАПИСАТЬ ТЕСТЫ!!!
;;**************************************************************************************************

;; TODO: НАПИСАТЬ ТЕСТЫ!!!
(defn if-empty?-row-or-nil?-val-then-row-else-do [field fn-p row]
  ;; Необходимо для операции save, чтобы возвращать пустоту при апдейте несуществующей строки
  (if (empty? row) row ; Если строка пустая то пропускаем ее
      (let [val (field row)] ; иначе вытаскиваем значение поля
        (if (nil? val) row ; если значение поля nil то пропускаем строку далее
            (assoc row field (fn-p val)))))) ; иначе применяем функцию к значению и обновляем запись

(defn prepare-as-string [field row] (if-empty?-row-or-nil?-val-then-row-else-do field name row))
(defn transform-as-keyword [field row] (if-empty?-row-or-nil?-val-then-row-else-do field keyword row))

;; ---------------------------------------

(defn prepare-clj-time-as-sql-time [field row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field #(tc/to-sql-time (tco/to-time-zone % (tco/default-time-zone))) row))

(defn transform-sql-time-as-clj-time [field row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field #(tco/to-time-zone (tc/from-sql-time %) (tco/default-time-zone)) row))

;; ---------------------------------------

(defn check-row [is-error-fn? message row]
  (if (is-error-fn? row) (throw (Exception. (str "Error on row -> " message "\nrow: " row)))
      row))

(defn check-row-field [field is-error-fn? message row]
  (if (is-error-fn? (field row))
    (throw (Exception. (str "Error on field:" field " -> " message "\nrow: " row)))
    row))

(defn print-row [row] (println "print=> " row) row)

;; END Entity utilites
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN Common utils
;;* tag: <common utils>
;;*
;;* description: Полезные утилиты
;;*
;;**************************************************************************************************

(defn print-debug->>>
  "Печать содержимого потока - нужно для отладки"
  [x & [message]]
  (println "BEGIN PRINT FOR > OR >> " (or message "") ":\n" x "\nEND PRINT")
  x)

;; need for tests
;; (def tree-list-1 [{:id 1 :parent_id nil}

;;                   {:id 2 :parent_id 1}

;;                   {:id 3 :parent_id 2}
;;                   {:id 4 :parent_id 3}
;;                   {:id 5 :parent_id 3}

;;                   {:id 6 :parent_id 2}
;;                   {:id 7 :parent_id 6}
;;                   {:id 8 :parent_id 6}

;;                   {:id 9 :parent_id 1}

;;                   {:id 10 :parent_id 9}
;;                   {:id 11 :parent_id 10}
;;                   {:id 12 :parent_id 10}

;;                   {:id 13 :parent_id 9}
;;                   {:id 14 :parent_id 13}
;;                   {:id 15 :parent_id 13}

;;                   {:id 16 :parent_id 1}

;;                   {:id 17 :parent_id 16}
;;                   {:id 18 :parent_id 17}
;;                   {:id 19 :parent_id 17}

;;                   {:id 20 :parent_id 16}
;;                   {:id 21 :parent_id 20}
;;                   {:id 22 :parent_id 20}

;;                   {:id 100 :parent_id 1000}
;;                   {:id 101 :parent_id 100}
;;                   {:id 102 :parent_id 100}
;;                   ])

;; TODO: написать тесты!
(defn add-tree-patches [get-id-fn get-parent-id-fn store-on-key tree-as-flat]
  (let [m (reduce #(assoc %1 (get-id-fn %2) (get-parent-id-fn %2)) {} tree-as-flat)]
    (letfn [(get-path [id]
              (loop [a (list id)]
                (let [parent-id (-> a first m)]
                  (if (nil? parent-id) a
                      (recur (conj a parent-id))))))]
      (map #(->> %1 get-id-fn get-path (assoc %1 store-on-key))
           tree-as-flat))))

(defn sort-tree-as-flat-groups
  "Сортировка дерева в списке по иерархии и группировка"
  [get-id-fn get-parent-id-fn tree-list]
  (let [roots (->> tree-list
                   (reduce (fn [[is ps] item]
                             [(conj is (get-parent-id-fn item))
                              (conj ps (get-id-fn item))])
                           [#{} #{}])
                   ((fn [[parents-ids ids]]
                      (let [roots-ids (clojure.set/difference parents-ids ids)]
                        (filter #(->> % get-parent-id-fn (contains? roots-ids)) tree-list)))) )]
    (letfn [(sort-tree [parent]
              (let [parent-id (get-id-fn parent)]
                (->> tree-list
                     (filter #(-> % get-parent-id-fn (= parent-id)))
                     (sort-by get-id-fn)
                     (reduce #(-> %1 (into (sort-tree %2))) [parent]) )))]
      (->> roots
           vec
           (sort-by get-id-fn)
           (reduce #(-> %1 (conj (sort-tree %2))) [] )))))

;; TODO: написать тесты!
(defn sort-tree-as-flat-groups-with-patches
  [get-id-fn get-parent-id-fn store-on-key tree-list]
  (->> tree-list
       (sort-tree-as-flat-groups get-id-fn get-parent-id-fn)
       (map (partial add-tree-patches get-id-fn get-parent-id-fn store-on-key))))

(defn sort-tree-as-flat
  "Сортировка древовидной структуры"
  [get-id-fn get-parent-id-fn tree-list]
  (->> tree-list
       (sort-tree-as-flat-groups get-id-fn get-parent-id-fn)
       (reduce into)))

;; END Common utils
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN files entity
;;* tag: <files entity>
;;*
;;* description: Хранение файлов
;;*
;;**************************************************************************************************

(def files-root-directory "files_root_directory")

(defn save-file [tempfile dir filename]
  (let [dir-filename (str dir filename)
        full-path (str files-root-directory dir-filename)]
    (clojure.java.io/make-parents full-path)
    (with-open [in (clojure.java.io/input-stream tempfile)
                out (clojure.java.io/output-stream full-path)]
      (try
        (do
          (clojure.java.io/copy in out)
          (.close in)
          (.close out))
        (catch Exception ex
          (do
            (.close in)
            (.close out)
            (throw ex) )))
      dir-filename)))


(defentity files
  (pk :id))

(defn files-save
  "Сохранение files"
  [file-row]
  (com-save-for-id files file-row))

(defn file-upload [file-row tempfile]
  (transaction
   (let [{id :id :as new-row} (-> file-row
                                  (assoc :path "?TMP?")
                                  files-save)
         path (save-file tempfile
                         (str (tf/unparse (tf/formatter-local "/yyyy/MM/dd/") (tl/local-now)) id "/")
                         (file-row :filename))]
     (-> new-row
         (assoc :path path)
         files-save))))

(defn file-pred-images* [query* & [not?]]
  (where query*
         {:content_type [(if not? not-in in) ["image/gif"
                                              "image/jpeg"
                                              "image/pjpeg"
                                              "image/png"
                                              "image/svg+xml"
                                              "image/tiff"]]}))


;;связи для файлов

(defentity files_rel
  (belongs-to files))

(defn file-upload-rel-on [entity files-rel-field {id :id :as entity-row} file-row tempfile]
  (transaction
   (let [file-row (file-upload file-row tempfile)]
     (insert files_rel (values {:files_id (:id file-row) files-rel-field id})))))

(defn files_rel-delete [files-rel-field {file-id :id} {rel-id :id}]
  (delete files_rel (where (and (= :files_id file-id)
                                (= files-rel-field rel-id)))))

(defn files_rel-select-files-by-* [row files-rel-field]
  ((com-defn-get-rows-by-rel* files :id :id files_rel :files_id files-rel-field) row))


;; END files entity
;;..................................................................................................



