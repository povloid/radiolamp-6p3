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
;;* BEGIN Translit
;;* tag: <translit>
;;*
;;* description: Транслитерация для веба
;;*
;;**************************************************************************************************

(def translit-table-ru-en
  (apply array-map [\a "a"
                    \b "b"
                    \c "c"
                    \d "d"
                    \e "e"
                    \f "f"
                    \g "g"
                    \h "h"
                    \i "i"
                    \j "j"
                    \k "k"
                    \l "l"
                    \m "m"
                    \n "n"
                    \o "o"
                    \p "p"
                    \q "q"
                    \r "r"
                    \s "s"
                    \t "t"
                    \u "u"
                    \v "v"
                    \w "w"
                    \x "x"
                    \y "y"
                    \z "z"

                    \1  "1"
                    \2  "2"
                    \3  "3"
                    \4  "4"
                    \5  "5"
                    \6  "6"
                    \7  "7"
                    \8  "8"
                    \9  "9"
                    \0  "0"

                    \а  "a"
                    \б  "b"
                    \в  "v"
                    \г  "g"
                    \д  "d"
                    \е  "e"
                    \ё  "e"
                    \ж  "zh"
                    \з  "z"
                    \и  "i"
                    \й  "j"
                    \к  "k"
                    \л  "l"
                    \м  "m"
                    \н  "n"
                    \о  "o"
                    \п  "p"
                    \р  "r"
                    \с  "s"
                    \т  "t"
                    \у  "u"
                    \ф  "f"
                    \х  "kh"
                    \ц  "c"
                    \ч  "ch"
                    \ш  "sh"
                    \щ  "shh"
                    \ъ  ""
                    \ы  "y"
                    \ь  ""
                    \э  "e"
                    \ю  "yu"
                    \я  "ya"
                    \space "-"
                    ]))

(defn make-translit [table s]
  (reduce #(str % (or (table %2) "")) ""  (clojure.string/lower-case s)))

(defn make-translit-ru-en [s]
  (make-translit translit-table-ru-en s))

;; END Translit
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN html to text
;;* tag: <html to text tools>
;;*
;;* description:
;;*
;;**************************************************************************************************


(def html-cleaner-props (doto (new org.htmlcleaner.CleanerProperties)
                          (.setTranslateSpecialEntities true)
                          (.setTransSpecialEntitiesToNCR true)
                          (.setTransResCharsToNCR true)
                          (.setOmitDeprecatedTags true)
                          (.setOmitComments true)))


(defn html-clean-tags [s]
  (when s
    (-> (org.htmlcleaner.HtmlCleaner. html-cleaner-props)
        (.clean s)
        .getText
        str
        (clojure.string/replace #"&nbsp;" " ")
        (clojure.string/replace #"&pound;" "£")
        (clojure.string/replace #"&euro;" "€")
        (clojure.string/replace #"&para;" "¶")
        (clojure.string/replace #"&sect;" "§")
        (clojure.string/replace #"&copy;" "©")
        (clojure.string/replace #"&reg;" "®")
        (clojure.string/replace #"&trade;" "™")
        (clojure.string/replace #"&deg;" "°")
        (clojure.string/replace #"&plusmn;" "±")
        (clojure.string/replace #"&times;" "×")
        (clojure.string/replace #"&divide;" "÷")
        (clojure.string/replace #"&fnof;" "ƒ")
        (clojure.string/replace #"&quot;" "\"")
        (clojure.string/replace #"&amp;" "&")
        (clojure.string/replace #"&lt;" "<")
        (clojure.string/replace #"&gt;" ">")
        (clojure.string/replace #"&hellip;" "…")
        (clojure.string/replace #"&prime;" "′")
        (clojure.string/replace #"&Prime;" "″")
        (clojure.string/replace #"&ndash;" "–")
        (clojure.string/replace #"&mdash;" "—")
        (clojure.string/replace #"&lsquo;" "‘")
        (clojure.string/replace #"&rsquo;" "’")
        (clojure.string/replace #"&sbquo;" "‚")
        (clojure.string/replace #"&ldquo;" "“")
        (clojure.string/replace #"&rdquo;" "”")
        (clojure.string/replace #"&bdquo;" "„")
        (clojure.string/replace #"&laquo;" "«")
        (clojure.string/replace #"&raquo;" "»")


        )))


;; END html to text tools
;;..................................................................................................



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


(defn prepare-date-to-sql-type [mk-java-sql-fn field row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field #(-> % .getTime mk-java-sql-fn) row))

(defn prepare-date-to-sql-date [field row]
  (prepare-date-to-sql-type #(new java.sql.Date %) field row))

(defn prepare-date-to-sql-time [field row]
  (prepare-date-to-sql-type #(new java.sql.Time %) field row))

(defn prepare-date-to-sql-timestamp [field row]
  (prepare-date-to-sql-type #(new java.sql.Timestamp %) field row))

(defn transform-sql-date-to-date [field row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field #(->> % .getTime (new java.util.Date)) row))

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
  ([get-id-fn get-parent-id-fn tree-list]
   (sort-tree-as-flat-groups get-id-fn get-parent-id-fn tree-list get-id-fn))
  ([get-id-fn get-parent-id-fn tree-list get-sort-field-fn]
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
                      (sort-by get-sort-field-fn)
                      (reduce #(-> %1 (into (sort-tree %2))) [parent]) )))]
       (->> roots
            vec
            (sort-by get-sort-field-fn)
            (reduce #(-> %1 (conj (sort-tree %2))) [] ))))))

;; TODO: написать тесты!
(defn sort-tree-as-flat-groups-with-patches
  ([get-id-fn get-parent-id-fn store-on-key tree-list]
   (sort-tree-as-flat-groups-with-patches get-id-fn get-parent-id-fn store-on-key tree-list get-id-fn))
  ([get-id-fn get-parent-id-fn store-on-key tree-list get-sort-field-fn]
   (->> (sort-tree-as-flat-groups get-id-fn get-parent-id-fn tree-list get-sort-field-fn)
        (map (partial add-tree-patches get-id-fn get-parent-id-fn store-on-key)))))

(defn sort-tree-as-flat
  "Сортировка древовидной структуры"
  ([get-id-fn get-parent-id-fn tree-list]
   (sort-tree-as-flat get-id-fn get-parent-id-fn tree-list get-id-fn))
  ([get-id-fn get-parent-id-fn tree-list get-sort-field-fn]
   (->> (sort-tree-as-flat-groups get-id-fn get-parent-id-fn tree-list get-sort-field-fn)
        (reduce into))))

;; END Common utils
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN entity webuser
;;* tag: <entity webuser>
;;*
;;* description: пользователь системы
;;*
;;**************************************************************************************************

(defentity webuser
  (pk :id)
  (transform (fn [row] (dissoc row :fts)))
  )


(defn webuser-save
  "Сохранить пользователя"
  [webuser-row]
  ;; SAVE
  (com-save-for-id webuser webuser-row))

(def webuser-select* (select* webuser))

(defn webuser-find-by-username [username]
  (-> (select* webuser)
      (where (= :username username))
      exec
      first))

(defn webuser-pred-search* [query s]
  (com-pred-full-text-search* query :fts s))

;; END entity webuser
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN entity webrole
;;* tag: <entity webrole>
;;*
;;* description:роли пользователей системы
;;*
;;**************************************************************************************************

(defentity webrole
  (pk :id)
  (prepare (fn [row] (-> row
                         ((partial prepare-as-string :keyname)))))
  (transform (fn [row] (-> row
                           ((partial transform-as-keyword :keyname))))))

(defn webrole-init [keyname title description]
  (com-save-for-field webrole :keyname
                      {:keyname (name keyname)
                       :title title
                       :description description}))


;; В принципе можно закэшировать !!!
(defn webrole-list [] (select webrole))



;; END entity webrole
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN entity webuserwebrole
;;* tag: <entity webuserwebrole>
;;*
;;* description: связь пользователей и ролей
;;*
;;**************************************************************************************************

(defentity webuserwebrole
  (pk :id))

(defn webuserwebrole-add-rel [webuser-row webrole-row]
  ((com-defn-add-rel-many-to-many :id :id webuserwebrole :webuser_id :webrole_id) webuser-row webrole-row))

                                        ;TODO: Надо написать тесты
(defn webuserwebrole-add-rels [{id :id :as webuser-row} webrole-rows]
  (transaction
   (delete webuserwebrole (where (= :webuser_id id)))
   (doall (map (partial webuserwebrole-add-rel webuser-row) webrole-rows))))

                                        ;TODO: Надо написать тесты
(defn webuserwebrole-add-rels-for-keynames [webuser-row webrole-rows-keynames]
  (->> webrole-rows-keynames
       (map #(first (select webrole (where (= :keyname (name %))))))
       (webuserwebrole-add-rels webuser-row)
       transaction))

(defn webuserwebrole-webuser-has-a-role? [webuser-row webrole-keyname]
  ((com-defn-has-a-rel? webrole :id :keyname webuserwebrole :webuser_id :webrole_id) webuser-row webrole-keyname))

(defn webuserwebrole-own-get-rels-set [webuser-row]
  ((com-defn-get-rels-set webrole :id :keyname webuserwebrole :webuser_id :webrole_id) webuser-row))

;; END entity webuserwebrole
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN files entity
;;* tag: <files entity>
;;*
;;* description: Хранение файлов
;;*
;;**************************************************************************************************
(def files-root-directory (promise))


(defn save-file [tempfile dir filename]
  (let [dir-filename (str dir "/" filename)
        full-path (str (if (realized? files-root-directory) @files-root-directory
                           (throw (Exception. "Значение пути  files-root-directory еще не задано")))
                       "/" dir-filename)]
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
  (pk :id)
  (transform (fn [row] (dissoc row :fts))))

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
                         (str (tf/unparse (tf/formatter-local "yyyy/MM/dd/") (tl/local-now)) id)
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

(defn files-pred-search* [query s]
  (com-pred-full-text-search* query :fts s))

;;связи для файлов

(defentity files_rel
  (belongs-to files))

(defn file-upload-rel-on [entity files-rel-field {id :id :as entity-row} file-row tempfile]
  (transaction
   (let [file-row (file-upload file-row tempfile)]
     [file-row (insert files_rel (values {:files_id (:id file-row) files-rel-field id}))]
     )))

(defn files_rel-delete [files-rel-field {file-id :id} {rel-id :id}]
  (delete files_rel (where (and (= :files_id file-id)
                                (= files-rel-field rel-id)))))

(defn files_rel-select-files-by-* [row files-rel-field]
  ((com-defn-get-rows-by-rel* files :id :id files_rel :files_id files-rel-field) row))


;; END files entity
;;..................................................................................................




;;**************************************************************************************************
;;* BEGIN tag entity
;;* tag: <tag entity>
;;*
;;* description: Товарные теги
;;*
;;**************************************************************************************************

;; Пример рекурсивного запроса с выдачей путей
;; WITH RECURSIVE temp1 ( id, parent_id, tagname ) AS (
;; SELECT id, parent_id, tagname, cast (tagname as text) as PATH FROM tag WHERE parent_id IS NULL
;; union
;; select a.id, a.parent_id, a.tagname, cast (b.PATH ||'->'|| a.tagname as text) as PATH FROM tag a, temp1 b WHERE a.parent_id = b.id)
;; select * from temp1 order by PATH

(defentity tag
  (pk :id)
  (prepare (fn [row] (-> row
                         ((partial prepare-as-string :constname)))))
  (transform (fn [row] (-> row
                           ((partial transform-as-keyword :constname))
                           (dissoc :fts)))))

(defn tag-const? [{id :id}]
  (-> (select* tag)
      (where (and (= :id id) (= :const true)))
      exec
      empty?
      not))

(defn tag-save-const [tag-row]
  (-> tag-row
      (assoc :const true)
      (update-in [:constname] name)
      ((partial com-save-for-field tag :constname))))

;; TODO: написать тесты
(defn tag-save
  "Сохранение tag"
  [tag-row]
  (transaction
   (if (tag-const? tag-row) (throw (Exception. "Константная запись не может быть изменена!"))
       (com-save-for-id tag (update-in tag-row [:constname] #(when % (name %)))))))

(defn tag-delete [{id :id :as tag-row}]
  (transaction
   (if (tag-const? tag-row) (throw (Exception. "Константная запись не может быть удалена!"))
       (com-delete-for-id tag id))))

;; TODO: написать тесты
(defn tag-get-tree-path [row]
  (transaction
   (let [row (com-find tag (:id row))]
     (if (nil? row) []
         (loop [i 0, {p-id :parent_id}, row a [row]]
           (cond (> i 10) (throw (Exception. "возможно обнаружена зацикленность графа либо глубина превышает 10"))
                 (nil? p-id) a
                 :else (let [next-row (com-find tag p-id)]
                         (recur (inc i) next-row (conj a next-row)))))))))

;; TODO: написать тесты
(defn tag-get-tree-childs [{id :id}]
  (select tag (where (= :parent_id id)) (order :tagname :ASC)))

;; TODO: написать тесты
(defn tag-set-parent [row {id :id :as parent-row}]
  (transaction
   (doseq [{i-id :id} (tag-get-tree-path parent-row)
           :when (= id i-id)]
     (do
       ;;(println i-id)
       (throw (Exception. "Попытка оторвать и замкнуть ветвь дерева"))))
   (-> row (assoc :parent_id id) tag-save)))

;; TODO: написать тесты
(defn tag-get-tree-path-and-childs [row]
  (transaction
   [(tag-get-tree-path row) (tag-get-tree-childs row)]))


(def tag-list* (select* tag))

;; TODO: написать тесты
(defn tag-tree-as-flat-groups []
  (sort-tree-as-flat-groups :id :parent_id (select tag)))

;; TODO: написать тесты
(defn tag-tree-as-flat-groups-with-patches [store-on-key]
  (sort-tree-as-flat-groups-with-patches :id :parent_id store-on-key (select tag) :tagname))

;; TODO: написать тесты
(defn tag-tree-as-flat []
  (sort-tree-as-flat :id :parent_id (select tag)))


(defn tag-select-all-sub-tree-ids [{id :id}]
  (transaction
   (letfn [(getch [id]
             (let [cc (map :id
                           (select tag
                                   (fields :id)
                                   (where (= :parent_id id))))]
               (reduce into cc (map getch cc))))]

     (getch id))))

(defn tag-select-all-sub-tree-ids-and-with-this-id [{id :id :as tag-row}]
  (conj (tag-select-all-sub-tree-ids tag-row) id))


(defn tag-pred-search* [query s]
  (com-pred-full-text-search* query :fts s))

;; END ctag entity
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN entity webdoc
;;* tag: <entity webdoc>
;;*
;;* description: Документы
;;*
;;**************************************************************************************************

(defentity webdoc
  (pk :id)
  (prepare (fn [{:keys [id keyname web_description] :as row}]
             (-> (if id row (assoc row :cdate (new java.util.Date)))
                 ((partial prepare-date-to-sql-timestamp :cdate))
                 (assoc :udate (new java.util.Date))
                 ((partial prepare-date-to-sql-timestamp :udate))
                 (assoc :ttitle (make-translit-ru-en (str keyname)))
                 (assoc :plan_text (html-clean-tags web_description))
                 )))

  (transform (fn [row]
               (-> row
                   ((partial transform-sql-date-to-date :cdate))
                   ((partial transform-sql-date-to-date :udate))
                   (dissoc :fts)
                   )))
  )



(defn webdoc-save
  "Сохранение webdoc"
  [webdoc-row]
  (com-save-for-id webdoc webdoc-row))

(def webdoc-select* (select* webdoc))

(declare webdoctag)
(defn webdoc-delete [{id :id}]
  (transaction
   (delete webdoctag (where (= :webdoc_id id)))
   (delete files_rel (where (= :webdoc_id id)))
   (com-delete-for-id webdoc id)))

;; END entity webdoc
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN entity webdoctag
;;* tag: <entity webdoctag>
;;*
;;* description: связь с тегами
;;*
;;**************************************************************************************************

(defentity webdoctag
  (belongs-to webdoc)
  (belongs-to tag))

;; TODO: написать тесты
(defn webdoctag-add-tag [webdoc-row tag-row]
  ((com-defn-add-rel-many-to-many :id :id webdoctag :webdoc_id :tag_id) webdoc-row tag-row))

(defn webdoctag-update-tags [{webdoc-id :id :as webdoc-row} tags-rows]
  (transaction
   (delete webdoctag (where (= :webdoc_id webdoc-id)))
   (->> tags-rows
        (map (fn [tag-row]
               (webdoctag-add-tag webdoc-row tag-row)))
        doall)))

;; TODO: написать тесты
(defn webdoctag-webdoc-has-a-tag? [webdoc-row tag-tagname]
  ((com-defn-has-a-rel? tag :id :tagname webdoctag :webdoc_id :tag_id) webdoc-row tag-tagname))

;; TODO: написать тесты
(defn webdoctag-webdoc-get-tags-set [webdoc-row & [field-for-set]]
  ((com-defn-get-rels-set tag :id (or field-for-set :tagname)
                          webdoctag :webdoc_id :tag_id) webdoc-row))


;; TODO: написать тесты
(defn webdoctag-select-webdocs-by-tag* [tag-row]
  ((com-defn-get-rows-by-rel* webdoc :id :id webdoctag :webdoc_id :tag_id) tag-row))

;; TODO: написать тесты
(defn webdoctag-select-webdocs-by-tag--nil-other*-se [tag-row ent]
  ((com-defn-get-rows-by-rel--nil-other* ent :id :id webdoctag :webdoc_id :tag_id) tag-row))



(defn webdoctag-select-webdocs-by-tag--nil-other* [tag-row]
  (webdoctag-select-webdocs-by-tag--nil-other*-se tag-row webdoc))

;; TODO: написать тесты
(defn webdoctag-tag-tree-as-flat-groups [webdoc-row]
  (let [webdoc-tags-ids-set (webdoctag-webdoc-get-tags-set webdoc-row :id)]
    (map (fn [tree-as-flat]
           (map #(assoc % :contain? (contains? webdoc-tags-ids-set (:id %)))
                tree-as-flat))
         (tag-tree-as-flat-groups))))

;; TODO: написать тесты
(defn webdoctag-tag-tree-as-flat-groups-with-patches [webdoc-row store-on-key]
  (let [webdoc-tags-ids-set (webdoctag-webdoc-get-tags-set webdoc-row :id)]
    (map (fn [tree-as-flat]
           (map #(assoc % :contain? (contains? webdoc-tags-ids-set (:id %)))
                tree-as-flat))
         (tag-tree-as-flat-groups-with-patches store-on-key))))




;; END entity webdoctag
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN webdoc predicates
;;* tag: <webdoc predicates>
;;*
;;* description: Предикаты для поиска документов
;;*
;;**************************************************************************************************

(defn webdoc-pred-search-for-the-child-tree-tags* [query tag-row]
  (let [tags-ids (tag-select-all-sub-tree-ids-and-with-this-id tag-row)]
    (println tags-ids)
    (where query
           (in :id (subselect webdoctag
                              (fields :webdoc_id)
                              (where (in :tag_id tags-ids)))))))

(defn webdoc-pred-search* [query s]
  (com-pred-full-text-search* query :fts s))


;; END webdoc predicates
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN stext entity
;;* tag: <any web text entity>
;;*
;;* description: для разного статического контента на сайте
;;*
;;**************************************************************************************************

(defentity stext
  (pk :id)
  (prepare (fn [row] (-> row
                         ((partial prepare-as-string :keyname)))))
  (transform (fn [row] (-> row
                           ((partial transform-as-keyword :keyname))
                           (dissoc :fts)))))

(defn stext-save [row]
  (com-save-for-id stext row))

(def stext-select* (select* stext))

(defn stext-pred-search* [query]
  (-> stext-select*
      (com-pred-full-text-search* :fts query)
      exec))

;; END anytext entity
;;..................................................................................................
