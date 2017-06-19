(ns r6p3.core
  (:use clojure.pprint)
  (:import [org.imgscalr Scalr])
  (:require [clojure.data :as clojure-data]
            [korma.db :as kdb]
            [korma.core :as kc]

            [clj-time.core :as tco]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clj-time.local :as tl]

            [image-resizer.core :refer :all]
            [image-resizer.format :as format]
            [image-resizer.pad :as pad]

            [clojure.edn :as edn]
            [cheshire.core :as cheshire-c]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))


(declare print-debug->>>)

(def development?
  (= "dev" (get (System/getenv) "IX_MODE")))

(def repl?
  (= "true" (get (System/getenv) "IX_REPL")))


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


(defn html-clean-tags
  "clean and replace string of html spec tags"
  [s]
  (when s
    (reduce

     (fn [s [m rs]]
       (clojure.string/replace s m rs))
     
     (-> (org.htmlcleaner.HtmlCleaner. html-cleaner-props)
         (.clean s)
         .getText
         str)

     (list [#"&nbsp;", " "]
           [#"&pound;", "£"]
           [#"&euro;", "€"]
           [#"&para;", "¶"]
           [#"&sect;", "§"]
           [#"&copy;", "©"]
           [#"&reg;", "®"]
           [#"&trade;", "™"]
           [#"&deg;", "°"]
           [#"&plusmn;", "±"]
           [#"&times;", "×"]
           [#"&divide;", "÷"]
           [#"&fnof;", "ƒ"]
           [#"&quot;", "\""]
           [#"&amp;", "&"]
           [#"&lt;", "<"]
           [#"&gt;", ">"]
           [#"&hellip;", "…"]
           [#"&prime;", "′"]
           [#"&Prime;", "″"]
           [#"&ndash;", "–"]
           [#"&mdash;", "—"]
           [#"&lsquo;", "‘"]
           [#"&rsquo;", "’"]
           [#"&sbquo;", "‚"]
           [#"&ldquo;", "“"]
           [#"&rdquo;", "”"]
           [#"&bdquo;", "„"]
           [#"&laquo;", "«"]
           [#"&raquo;", "»"]))))


;; END html to text tools
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN SQL TOOLS
;;* tag: <sql tools>
;;*
;;* description: Sql tools fns
;;*
;;**************************************************************************************************

(defmacro try-catch [& body]
  `(try
     (do ~@body)
     (catch java.sql.BatchUpdateException ex-buth#
       (let [ex# (.getNextException ex-buth#)]
         (-> ex#
             .getMessage
             ((fn [message#] (println message#) message#)))))
     (catch Exception ex#
       (do
         (clojure.stacktrace/print-stack-trace ex#)
         (-> ex#
             .getMessage
             ((fn [message#] (println message#) message#)))))))


;; COMMON FUNCTIONS ----------------------------------------------------

(defn com-exec
  "Выполнить запрос"
  [query]
  (kc/exec query))

(defn com-exec-1
  "Выполнить запрос одной записи"
  [query]
  (-> query
      (kc/limit 1)
      kc/exec
      first))



(defn com-save-for-field
  "Сохранить сущность"
  [entity field row]
  (kdb/transaction
   (let [r (kc/update entity (kc/set-fields row) (kc/where (= field (row field))))]
     (if (= r 0)
       (kc/insert entity (kc/values row))
       (first (kc/select entity (kc/where (= field (row field)))))))))

(defn com-save-for-field-2
  "Сохранить сущность"
  [entity field-1 field-2 row]
  (kdb/transaction
   (let [r (kc/update entity (kc/set-fields row) (kc/where (and
                                                            (= field-1 (row field-1))
                                                            (= field-2 (row field-2))
                                                            )))]
     (if (= r 0)
       (kc/insert entity (kc/values row))
       (first (kc/select entity (kc/where (and
                                           (= field-1 (row field-1))
                                           (= field-2 (row field-2))))))))))

(defn com-save-for-field-3
  "Сохранить сущность"
  [entity field-1 field-2 field-3 row]
  (kdb/transaction
   (let [r (kc/update entity (kc/set-fields row) (kc/where (and
                                                            (= field-1 (row field-1))
                                                            (= field-2 (row field-2))
                                                            (= field-3 (row field-3)))))]
     (if (= r 0)
       (kc/insert entity (kc/values row))
       (first (kc/select entity (kc/where (and
                                           (= field-1 (row field-1))
                                           (= field-2 (row field-2))
                                           (= field-3 (row field-3))))))))))

(defn com-save-for-id
  "Сохранить сущность"
  [entity row]
  (kdb/transaction
   (let [r (kc/update entity (kc/set-fields row) (kc/where (= :id (row :id))))]
     (if (= r 0)
       (kc/insert entity (kc/values row))
       (first (kc/select entity (kc/where (= :id (row :id)))))))))

(defn com-delete*
  "Удалить сущность"
  [entity]
  (kc/delete* entity))

(defn com-delete-for-id
  "Удалить сущность"
  [entity]
  (kc/delete* entity))

(defn com-delete-for-id
  "Удалить сущность по id"
  [entity id]
  (kc/delete entity (kc/where (= :id id))))

(defn com-find
  "Найти сущность по :id"
  [entity id]
  (let [rows (kc/select entity (kc/where (= :id id)))]
    (cond (empty? rows) nil
          (< 1 (count rows)) (do
                               (println "Внимание! По com-find нашел более одной записи по id "
                                        id "но выдал только первую")
                               (first rows))
          :else (first rows))))

(defn com-find*-1
  "Найти сущность по :id"
  [select*-1 id]
  (-> select*-1
      (kc/where (= :id id))
      com-exec-1))

(defn com-count
  "Количество всех элементов"
  [entity]
  (-> (kc/select entity (kc/aggregate (count :*) :c))
      first
      :c))

(defn com-count*
  "Количество всех элементов"
  [select*-1]
  (-> select*-1
      (kc/aggregate (count :*) :c)
      com-exec-1
      :c))

(defn com-pred-page* [query* page size]
  (-> query* (kc/limit size) (kc/offset (* page size))))


;;------------------------------------------------------------------------------
;; BEGIN: Специфичный для postgresql код
;; tag: <postgresql sql specific>
;; description:
;;------------------------------------------------------------------------------


;;----------------------------------------------------------
;; BEGIN: Полнотекстовый поиск
;; tag: <postgresql sql specific fts>
;; description:
;;----------------------------------------------------------

(def spec-word-or #"\|")

(def spec-query-prefix "tsquery//")
(def spec-query-prefix-count (count spec-query-prefix))


(defn- fts-simpl-query-prep [fts-query {:keys [term-cond] :or {term-cond \&}}]
  (->> (clojure.string/split (str fts-query) spec-word-or)
       (map (fn [s]
              (->> (clojure.string/split (clojure.string/trim s) #"\s+")
                   (map #(str % ":*"))
                   (reduce #(str %1 \space term-cond \space %2))
                   (#(str "(" % ")")))))
       (clojure.string/join " | ")))

(defn com-pred-full-text-search*-term
  ([fts-field-ds fts-query]
   (com-pred-full-text-search*-term fts-field-ds fts-query {}))
  ([fts-field-ds fts-query opts]
   (let [fts-query (clojure.string/trim fts-query)
         fts-query (if (.startsWith fts-query spec-query-prefix)
                     (-> fts-query (subs spec-query-prefix-count) clojure.string/trim)
                     (fts-simpl-query-prep fts-query opts))]
     (kc/raw (str " "
                  (if (coll? fts-field-ds)
                    (str "concat_ws(' ', " ;; Нужно делать через функцию concat_ws потому что если попадает null то все будет null
                         ;; при конкатенации через ||
                         (->> fts-field-ds (map name)
                              (clojure.string/join ",")) ")")
                    (name fts-field-ds))
                  " @@ to_tsquery('" fts-query "')")))))

(defn com-pred-full-text-search* [query* fts-field fts-query]
  (kc/where query* (com-pred-full-text-search*-term fts-field fts-query)))



;; END Полнотекстовый поиск
;;..........................................................

;;----------------------------------------------------------
;; BEGIN: Функционал для работы с деревьями
;; tag: <postgresql sql specific tree>
;; description:
;;----------------------------------------------------------

(defn pg--raw-make-fields-str
  "Конструирование строки полей для SQL SELECT"
  ([fields]
   (pg--raw-make-fields-str nil fields))
  ([select-table-alias fields]
   (let [c                  (dec (count fields))
         select-table-alias (if select-table-alias (str (name select-table-alias) ".") "")]
     (->> fields
          (map-indexed (fn [i field]
                         (str select-table-alias
                              (name field)
                              (if (= i c) "" ", "))))
          (apply str)))))

(defn pg--raw--select-tree-sub-childs
  "Выбрать всех потомков конкретного узла дерева"
  ([row table]
   (pg--raw--select-tree-sub-childs row table [:id :parent_id :keyname]))
  ([{:keys [id]} table fields]
   (kc/exec-raw [(str "
WITH RECURSIVE r AS (
   SELECT " (pg--raw-make-fields-str :a fields) "
   FROM " table " a
   WHERE parent_id = ?

   UNION

   SELECT " (pg--raw-make-fields-str :b fields) "
   FROM " table " b JOIN r ON b.parent_id = r.id
)

SELECT * FROM r;
") [id]] :results)))


(defn pg--raw--select-tree-parents
  "Выбрать всех предков конкретного узла дерева, включая его самого
  (формирует порядок от младшего к старшему)"
  ([row table]
   (pg--raw--select-tree-parents row table [:id :parent_id :keyname]))
  ([{:keys [id]} table fields]
   (kc/exec-raw [(str "
WITH RECURSIVE r AS (
   SELECT " (pg--raw-make-fields-str :a fields) "
   FROM " table " a
   WHERE id = ?

   UNION

   SELECT " (pg--raw-make-fields-str :b fields) "
   FROM " table " b JOIN r ON b.id = r.parent_id
)

SELECT * FROM r;
") [id]] :results)))

;; END Функционал для работы с деревьями
;;..........................................................


;; END Специфичный для postgresql код
;;..............................................................................





(defn com-defn-add-rel-many-to-many
  "создание функции соединения двух сущностей по типу many-to-many"
  [field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-1-id field-id-1} {entity-2-id field-id-2}]
    (let [entity-2-row (kc/select rel-entity
                                  (kc/where (and
                                             (=  field-fk-id-1 entity-1-id)
                                             (=  field-fk-id-2 entity-2-id))))]
      (if (empty? entity-2-row) (kc/insert rel-entity
                                           (kc/values {field-fk-id-1 entity-1-id field-fk-id-2 entity-2-id}))
          (first entity-2-row)))))



(defn com-defn-has-a-rel?
  "создание функции проверки наличия соединения двух сущностей по типу many-to-many"
  [entity-2 field-id-2 field-keyname-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-1-id field-id-2} entity-2-keyname]
    (kdb/transaction
     (let [entity-2s-ids (kc/select entity-2
                                    (kc/fields field-id-2)
                                    (kc/where (= field-keyname-2 (name entity-2-keyname))))]
       (if (empty? entity-2s-ids)
         (throw (Exception. (str "Row with " field-keyname-2 " = " entity-2-keyname " is not described.")))
         (not (empty? (kc/select rel-entity
                                 (kc/limit 1)
                                 (kc/where (and
                                            (= field-fk-id-1 entity-1-id)
                                            (= field-fk-id-2 (-> entity-2s-ids first field-id-2))))))))))))

(defn com-defn-get-rels-set
  [entity-2 field-id-2 field-keyname-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [{entity-1-id field-id-2}]
    (kdb/transaction
     (->> (kc/select entity-2 (kc/fields field-keyname-2)
                     (kc/where {field-id-2 [in (kc/subselect rel-entity
                                                             (kc/fields field-fk-id-2)
                                                             (kc/where (= field-fk-id-1 entity-1-id)))]}))
          (map field-keyname-2)
          set))))

(defn com-defn-pred-rows-by-rel?
  [field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2 & [not?]]
  (fn [select*-1 {entity-2-id field-id-2}]
    (kc/where select*-1
              {field-id-1
               (let [p [in (kc/subselect rel-entity
                                         (kc/fields field-fk-id-1)
                                         (kc/where (= field-fk-id-2 entity-2-id)))]]
                 (if not? (not p) p))})))

(defn com-defn-pred-rows-by-rel--nil-other?
  [field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2]
  (fn [select*-1 {entity-2-id field-id-2 :as row}]
    (if (nil? entity-2-id)
      ;; nil
      (kc/where select*-1 (not (exists
                                (kc/subselect rel-entity
                                              (kc/where (= field-fk-id-1
                                                           (-> select*-1
                                                               :ent
                                                               :table
                                                               str
                                                               (str "." (name field-id-1))
                                                               keyword)
                                                           ))))))
      ;; not nil
      ((com-defn-pred-rows-by-rel? field-id-1 field-id-2
                                   rel-entity field-fk-id-1 field-fk-id-2)
       select*-1 row))))

(defn com-defn-pred-rows-by-rels?
  [field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2 & [not?]]
  (fn [select*-1 entity-2-rows]
    (reduce
     (fn [query entity-2-row]
       (kc/where query
                 (let [p (exists
                          (kc/subselect rel-entity
                                        (kc/fields field-fk-id-1)
                                        (kc/where (and
                                                   (= field-fk-id-2 (field-id-2 entity-2-row))
                                                   (= field-fk-id-1 (-> select*-1
                                                                        :ent
                                                                        :table
                                                                        (str "." (name field-id-1))
                                                                        keyword)))
                                                  )))]
                   (if not? (not p) p))))
     select*-1 entity-2-rows)))




(defn com-defn-select-all-many-to-many--and-add-true-value-to-each-row-if-row-selected
  "создание функции соединения двух сущностей по типу many-to-many"
  [field-id-1 field-id-2 rel-entity field-fk-id-1 field-fk-id-2 entity-2 row-selected-field]
  (fn [{entity-1-id field-id-1}]
    (let [s-selected (->> (kc/select rel-entity
                                     (kc/where (=  field-fk-id-1 entity-1-id)))
                          (reduce (fn [a {id field-fk-id-2}] (conj a id)) #{}))]

      (->> (kc/select entity-2)
           (map (fn [{id field-id-2 :as row}]
                  (assoc row row-selected-field (contains? s-selected id))))))))




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
;; cdate

(defn prepare-cdate [{id :id :as row}]
  (let [date (new java.util.Date)]
    (-> (if id
          row
          (prepare-date-to-sql-timestamp :cdate (assoc row :cdate date))))))


(defn transform-cdate [row]
  (transform-sql-date-to-date :cdate row))



;; ---------------------------------------
;; cdate and udate - maker

(defn prepare-cdate-udate [{id :id :as row}]
  (let [date (new java.util.Date)]
    (->> (-> (if id
               row
               (prepare-date-to-sql-timestamp :cdate (assoc row :cdate date)))
             (assoc :udate date))
         (prepare-date-to-sql-timestamp :udate))))


(defn transform-cdate-udate [row]
  (->> row
       (transform-sql-date-to-date :cdate)
       (transform-sql-date-to-date :udate)))

;; ---------------------------------------
;; JSON transformations


(defn prepare-as-json  [field row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field cheshire-c/generate-string row))

(defn transform-as-clj [field keywordize? row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field
   (fn [v]
     (cheshire-c/parse-string (str v) keywordize?))
   row))


;; ---------------------------------------
;; EDN transformations

(defn prepare-as-edn  [field row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field prn-str row))

(defn transform-edn-as-clj [field row]
  (if-empty?-row-or-nil?-val-then-row-else-do
   field
   (fn [v]
     (edn/read-string (str v)))
   row))

;; ---------------------------------------
;; Translit

(defn prepare-field-to-translit-field [field to-field row]
  (if-let [v (row field)]
    (assoc row to-field (make-translit-ru-en v))
    row))

;; ---------------------------------------


(defn check-row [is-error-fn? message row]
  (if (is-error-fn? row) (throw (Exception. (str "Error on row -> " message "\nrow: " row)))
      row))

(defn check-row-field [field is-error-fn? message row]
  (if (is-error-fn? (field row))
    (throw (Exception. (str "Error on field:" field " -> " message "\nrow: " row)))
    row))

(defn print-row [row] (println "print=> " row) row)



;;------------------------------------------------------------------------------
;; BEGIN: entity json state
;; tag: <entity json state>
;; description: Состояние сущности в cljs-json
;;------------------------------------------------------------------------------

(defn entity-state-to-json [row]
  (cheshire-c/generate-string row))

(defn entity-state-from-json [text]
  (cheshire-c/parse-string (str text) true))


(defn entity-states-diff [new-row old-row prep-fn]
  (clojure-data/diff (prep-fn new-row) (prep-fn old-row)))

(defn entity-states-diff-to-json [new-row old-row prep-fn]
  (cheshire-c/generate-string (entity-states-diff new-row old-row prep-fn)))


;; END entity json state
;;..............................................................................


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
;;* BEGIN files entity
;;* tag: <files entity>
;;*
;;* description: Хранение файлов
;;*
;;**************************************************************************************************
(def files-root-directory (promise))


(defn image-file_as_w_png [buffered-file file-src w]
  (let [resized-file (new java.io.File (str (.getPath file-src) "_as_" w ".png"))]
    (javax.imageio.ImageIO/write buffered-file "png" resized-file)
    (.getAbsolutePath resized-file)))

(defn save-file-o [tempfile dir filename {:keys [ws] :or {ws []}}]
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
            (throw ex))))

      (let [file-src (new java.io.File full-path)
            ext (image-resizer.fs/extension full-path)]
        ;;(println "EXT:" (clojure.string/lower-case ext))
        (when (#{"png" "jpg" "jpeg" "gif"} (clojure.string/lower-case ext))
          (doseq [w ws]
            (-> file-src
                ;;(image-resizer.core/resize-to-width w)

                ((image-resizer.resize/resize-fn w w image-resizer.scale-methods/ultra-quality))

                ;;(javax.imageio.ImageIO/write ext (new java.io.File full-path-spec))
                ;;(format/as-file full-path-spec)
                (image-file_as_w_png file-src w)
                ))))

      dir-filename)))


(declare files)

(defn files-resize-all-images [{:keys [ws] :or {ws []}}]
  (doseq [{path :path} (kc/select files
                                  (kc/fields :path)
                                  (kc/where {:content_type [in ["image/gif"
                                                                "image/jpeg"
                                                                "image/pjpeg"
                                                                "image/png"
                                                                ;;"image/svg+xml"
                                                                ;;"image/tiff"
                                                                ]]}))
          :let [path (clojure.string/replace-first path #"/image/" "/")
                file-src (new java.io.File (str @files-root-directory "/" path))]]
    (do
      (println "Do file: " (.getPath file-src))
      (doseq [w ws]
        (-> file-src
            ;;(image-resizer.core/resize-to-width w)

            ((image-resizer.resize/resize-fn w w image-resizer.scale-methods/ultra-quality))

            ;;(format/as-file full-path-spec)
            (image-file_as_w_png file-src w)

            println))
      (println "OK"))))

(defn save-file [tempfile dir filename]
  (save-file-o tempfile dir filename {}))

(kc/defentity files
  (kc/pk :id)
  (kc/transform (fn [row] (dissoc row :fts))))

(def files-select* (kc/select* files))

(defn files-save
  "Сохранение files"
  [file-row]
  (com-save-for-id files file-row))

(defn files-delete [row]
  (com-delete-for-id files (:id row)))

(defn file-upload-o [file-row tempfile {:keys [path-prefix]
                                        :or {path-prefix ""}
                                        :as options}]
  (kdb/transaction
   (let [{id :id :as new-row} (-> file-row
                                  (assoc :path "?TMP?")
                                  files-save)
         path (save-file-o tempfile
                           (str (tf/unparse (tf/formatter-local "yyyy/MM/dd/") (tl/local-now)) id)
                           (file-row :filename)
                           options)]
     (-> new-row
         (assoc :path (str path-prefix path))
         files-save))))

(defn file-upload [file-row tempfile]
  (file-upload-o file-row tempfile {}))


(def images-content-types #{"image/gif"
                            "image/jpeg"
                            "image/pjpeg"
                            "image/png"
                            "image/svg+xml"
                            "image/tiff"})

(defn in-row--files-rows-split-by-content-types [{files :files :as row}]
  (reduce
   (fn [{:keys [files images] :as  row}
        {content_type :content_type :as file-row}]
     (if (contains? images-content-types content_type)
       (assoc row :images (conj images file-row))
       (assoc row :files  (conj files  file-row))))
   (assoc row :images [] :files [])
   files))

(defn in-rows--files-rows-split-by-content-types [rows]
  (map in-row--files-rows-split-by-content-types rows))


(defn file-pred-images* [query* & [not?]]
  (kc/where query*
            {:content_type [(if not? not-in in) images-content-types]}))

(defn file-pred-galleria? [query*]
  (kc/where query* (= :galleria true)))



(defn files-pred-search? [select*-1 fts-query]
  (com-pred-full-text-search* select*-1 :fts fts-query))

;;связи для файлов

(kc/defentity files_rel
  (kc/belongs-to files))

(defn file-upload-rel-on-o [entity files-rel-field {id :id :as entity-row} options file-row tempfile]
  (kdb/transaction
   (let [file-row (file-upload-o file-row tempfile options)]
     [file-row (kc/insert files_rel
                          (kc/values {:files_id (:id file-row) files-rel-field id}))]
     )))

(defn file-upload-rel-on [entity files-rel-field {id :id :as entity-row} file-row tempfile]
  (file-upload-rel-on-o entity files-rel-field {id :id :as entity-row} {} file-row tempfile))



(defn files_rel-delete [files-rel-field {file-id :id} {rel-id :id}]
  (kc/delete files_rel (kc/where (and (= :files_id file-id)
                                      (= files-rel-field rel-id)))))

(defn files_rel-select-files-by-* [row files-rel-field]
  (-> ((com-defn-pred-rows-by-rel? :id :id files_rel :files_id files-rel-field)
       files-select* row)
      (kc/order :id)))


(defn web-file-upload [service-upload-fn uploader-params]
  (letfn [(upload-one [{:keys [size tempfile content-type filename]}]
            (service-upload-fn {:filename filename :size size :content_type content-type} tempfile))]
    (if (map? uploader-params) (upload-one uploader-params)
        (doall (map upload-one uploader-params)))))



;; END files entity
;;..................................................................................................





;;**************************************************************************************************
;;* BEGIN entity webuser
;;* tag: <entity webuser>
;;*
;;* description: пользователь системы
;;*
;;**************************************************************************************************

(kc/defentity webuser
  (kc/pk :id)
  (kc/transform (fn [row] (dissoc row :fts :password))))

(kc/defentity webuser!!!
  (kc/table :webuser)
  (kc/pk :id)
  (kc/transform (fn [row] (dissoc row :fts))))


(defn webuser-save
  "Сохранить пользователя"
  [webuser-row]
  (com-save-for-id webuser webuser-row))

(defn webuser-save-for-username
  "Сохранить пользователя по имени"
  [webuser-row]
  (com-save-for-field webuser :username webuser-row))

(def webuser-select* (kc/select* webuser))

(defn webuser-find-by-username [username]
  (-> webuser-select*
      (kc/where (= :username username))
      com-exec-1))

(defn webuser-find-by-username!!! [username]
  (-> webuser!!!
      kc/select*
      (kc/where (= :username username))
      com-exec-1))


(defn webuser-pred-search? [select*-1 fts-query]
  (com-pred-full-text-search* select*-1 :fts fts-query))


(defn webuser-get-username-from-request [request]
  (-> request
      :session
      :cemerick.friend/identity
      :current))


(defn webuser-get-from-request [request]
  (-> request
      webuser-get-username-from-request
      webuser-find-by-username))

;; END entity webuser
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN entity webrole
;;* tag: <entity webrole>
;;*
;;* description:роли пользователей системы
;;*
;;**************************************************************************************************


(kc/defentity webrolesgroup
  (kc/pk :id)
  (kc/prepare (partial prepare-as-string :keyname))
  (kc/transform (partial transform-as-keyword :keyname)))


(defn webrolesgroup-init [row]
  (com-save-for-field webrolesgroup :keyname (update-in row [:keyname] name)))


(kc/defentity webrole
  (kc/pk :id)
  (kc/belongs-to webrolesgroup)
  (kc/prepare (partial prepare-as-string :keyname))
  (kc/transform (partial transform-as-keyword :keyname)))

(defn webrole-init [row]
  (com-save-for-field webrole :keyname (update-in row [:keyname] name)))

(defn webrole-list
  ([] (webrole-list false))
  ([with-webrolesgroup]
   (if with-webrolesgroup
     (kc/select webrole (kc/with webrolesgroup))
     (kc/select webrole))))


;; END entity webrole
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN entity webuserwebrole
;;* tag: <entity webuserwebrole>
;;*
;;* description: связь пользователей и ролей
;;*
;;**************************************************************************************************

(kc/defentity webuserwebrole
  (kc/pk :id))

(defn webuserwebrole-add-rel [webuser-row webrole-row]
  ((com-defn-add-rel-many-to-many :id :id webuserwebrole :webuser_id :webrole_id) webuser-row webrole-row))

                                        ;TODO: Надо написать тесты
(defn webuserwebrole-add-rels [{id :id :as webuser-row} webrole-rows]
  (kdb/transaction
   (kc/delete webuserwebrole (kc/where (= :webuser_id id)))
   (doall (map (partial webuserwebrole-add-rel webuser-row) webrole-rows))))

                                        ;TODO: Надо написать тесты
(defn webuserwebrole-add-rels-for-keynames [webuser-row webrole-rows-keynames]
  (->> webrole-rows-keynames
       (map #(first (kc/select webrole (kc/where (= :keyname (name %))))))
       (webuserwebrole-add-rels webuser-row)
       kdb/transaction))

(defn webuserwebrole-webuser-has-a-role? [webuser-row webrole-keyname]
  ((com-defn-has-a-rel? webrole :id :keyname webuserwebrole :webuser_id :webrole_id) webuser-row webrole-keyname))

(defn webuserwebrole-own-get-rels-set [webuser-row]
  ((com-defn-get-rels-set webrole :id :keyname webuserwebrole :webuser_id :webrole_id) webuser-row))

(defn webuserwebrole-get-rels-set-for-username [username]
  (-> username
      webuser-find-by-username
      webuserwebrole-own-get-rels-set
      kdb/transaction))

(defn webuserwebrole-get-rels-set-from-request [request]
  (-> request
      :session
      :cemerick.friend/identity
      :current
      webuserwebrole-get-rels-set-for-username ))

(defn contains-webroles? [roles-keys-set roles]
  (reduce #(or % (contains? roles-keys-set (:keyname %2))) false roles))

(defn keys-for-roles [row roles-keys-set roles ks]
  (if (contains-webroles? roles-keys-set roles)
    row (reduce dissoc row ks)))



(defn is-allow-from-request [request roles]
  (-> request
      webuserwebrole-get-rels-set-from-request
      (contains-webroles? roles)))


(defn throw-when-no-role [roles-keys-set role]
  (when (not (contains? roles-keys-set (:keyname role)))
    (throw (Exception. "У вас нет прав доступа для данной функции!"))))

(defn throw-when-no-roles [roles-keys-set roles]
  (when (not (contains-webroles? roles-keys-set roles))
    (throw (Exception. "У вас нет прав доступа для данной функции!"))))

(defn throw-when-no-role-from-request [request webrole-row]
  (throw-when-no-role
      (webuserwebrole-get-rels-set-from-request request)
    webrole-row))




;; дополнительный функционал
(defn add-user [username password roles-keys-set]
  (let [u (webuser-save {:username username
                         :password (cemerick.friend.credentials/hash-bcrypt password)})]
    (webuserwebrole-add-rels-for-keynames u roles-keys-set)))




;; END entity webuserwebrole
;;..................................................................................................




