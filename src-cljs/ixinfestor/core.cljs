(ns ixinfestor.core
  (:require-macros [cljs.core.async.macros :refer [go ]])

  (:require [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]

            [hipo.core :as hipo]

            [ajax.core :refer [GET POST]]
            [dommy.core :as dommy :refer-macros [sel sel1]]

            [goog.i18n.DateTimeFormat :as dtf]))

(enable-console-print!)


;; (defn convert-to-array [js-col]
;;   (-> (clj->js [])
;;       (.-slice)
;;       (.call js-col)
;;       (js->clj)))

;;**************************************************************************************************
;;* BEGIN date and time functions
;;* tag: <date and time functions>
;;*
;;* description: Работа с датами и временем
;;* примеры
;; 1 форматирование
;;(.format (goog.i18n.DateTimeFormat. (.-LONG_DATE goog.i18n.DateTimeFormat.Format)) (js/Date.))
;; 2 получение из стороки и форматирование
;;(.format (goog.i18n.DateTimeFormat. (.-LONG_DATE goog.i18n.DateTimeFormat.Format))
;;  (new js/Date (.parse js/Date (get-in row [:webdoc-row :udate]))))
;;**************************************************************************************************

(def date-formats
  (let [f goog.i18n.DateTimeFormat.Format]
    {:FULL_DATE (.-FULL_DATE f)
     :FULL_DATETIME (.-FULL_DATETIME f)
     :FULL_TIME (.-FULL_TIME f)
     :LONG_DATE (.-LONG_DATE f)
     :LONG_DATETIME (.-LONG_DATETIME f)
     :LONG_TIME (.-LONG_TIME f)
     :MEDIUM_DATE (.-MEDIUM_DATE f)
     :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
     :MEDIUM_TIME (.-MEDIUM_TIME f)
     :SHORT_DATE (.-SHORT_DATE f)
     :SHORT_DATETIME (.-SHORT_DATETIME f)
     :SHORT_TIME (.-SHORT_TIME f)
     }))


(defn str-to-date [date-string]
  (new js/Date (.parse js/Date date-string)))

(defn format-date
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
  or a formatting string like \"dd MMMM yyyy\"
  examples:
  > (format-date-generic :LONG_DATE (js/Date.))
  > \"July 14, 2012\"
  > (format-date-generic \"dd MMMM yyyy\" (js/Date.))
  > \"14 July 2012\"
  > (format-date-generic \"MMMM\" (js/Date.))
  > \"July\"
  "
  [date-format date]
  (.format (goog.i18n.DateTimeFormat. (or (date-formats date-format) date-format))  date))

(defn str-to-date-and-format [date-format alt-string s]
  (if (nil? s) alt-string
      (format-date date-format (str-to-date s))))

;; END date and time functions
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN ID functions
;;* tag: <id>
;;*
;;* description: формирование идентификаторов
;;*
;;**************************************************************************************************

;; ! Код пока дублируется !

(defn- keyword-replace-char [kw c as-c]
  (->> kw name vec
       (map #(if (= % c) as-c %))
       (reduce str)
       keyword))

(defn xml-id [kw & parent]
  (if (empty? parent) (keyword-replace-char kw \- \_)
      (-> parent
          first
          name
          (str "--" (name kw))
          (keyword-replace-char \- \_))))

;; ! Код пока дублируется !

(defn by-id
  "Short-hand for document.getElementById(id)"
  [id]
  (.getElementById js/document (name id)))

;; END ID functions
;;..................................................................................................

(defn events->chan
  "Given a target DOM element and event type return a channel of
  observed events. Can supply the channel to receive events as third
  optional argument."
  ([el event-type] (events->chan el event-type (chan)))
  ([el event-type c]
   (events/listen el event-type
                  (fn [e] (put! c e)))
   c))

(declare modal-cr-and-show-on-id)

(defn error-handler [{:keys [status status-text] :as erow}]
  (.log js/console (str "Error: " erow))

  (when (by-id :modal-error)
    (modal-cr-and-show-on-id
     :modal-error
     {:title  [:div {:class "alert alert-danger" :role "alert"}
               [:span {:class "glyphicon glyphicon-exclamation-sign" :aria-hidden "true"}]
               (str " Ошибка " status " : " status-text)]
      :body   [:div {:class "row"}
               [:div {:id "modal-error-body" :class "col-sm-12"} ]]
      :footer [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"} "Закрыть"]
      })

    (dommy/set-html!
     (by-id :modal-error-body)
     (-> erow :parse-error :original-text))

    ;;  (js/alert (str "Ошибка операции" status))
    ))

(defn display-message-on-time [time message]
  (let [el [:div {:id "top-message" :class "alert alert-success" :style "position:absolute;bottom:10px;right:10px" :role "alert"}
            message]]
    (->> el hipo/create (dommy/append! (sel1 :body)))
    ;;(.appendChild (.-body js/document) (hipo/create el))
    (js/setTimeout (fn []
                     (when-let [e (by-id :top-message)]
                       (dommy/remove! e)))
                   time)))



(defn m-not=vals [old new k default]
  (or (not= (k old) (k new)) (= (k new) default)))

(defn parse-int [str-val default-val]
  (if (nil? str-val) default-val (js/parseInt str-val)))


(defn drop-long-string [str limit]
  (if (<= (count str) limit) str
      (str (subs str 0 (- limit 4)) "...")))

(defn keyword-i [k i]
  (let [z (keyword (str (name k) i))]
    ;;(.log js/console (str z))
    z))


;;**************************************************************************************************
;;* BEGIN page state
;;* tag: <page state>
;;*
;;* description: состояние страници
;;*
;;**************************************************************************************************

(defn ix-set [params]
  (GET "/ix/set"
       {:params params
        :error-handler error-handler}))


(defn page-state-cr []
  (let [page-state (atom {})]
    (add-watch page-state :ix-set
               (fn [_ _ old new]
                 (when (not= old new)
                   (ix-set @page-state))))
    page-state))


(defn make-init-page [page-state params m f]
  (when (and js/document (.-getElementById js/document))
    (let [params-clj (js->clj params :keywordize-keys true)]
      (.log js/console (str "params-clj >>> " params-clj))

      ;;(.log js/console (str "init-map   >>> " init-map))

      (->> m
           seq
           (reduce (fn [params-clj [k f]] (update-in params-clj [k] f))  params-clj)
           (swap! page-state merge)
           println)

      (f params-clj))))


(defn add-watch-ix-set [page-state]
  (add-watch page-state :ix-set
             (fn [_ _ old new] (when (not= old new) (ix-set @page-state)))))

(defn add-watch-on-update-for
  ([page-state watch-key value-path do-fn]
   (add-watch-on-update-for page-state watch-key value-path nil do-fn))
  ([page-state watch-key value-path window-onload-fn do-fn]
   ;;   (swap! page-state assoc-in value-path init-value)
   (when window-onload-fn
     (set! (.-onload js/window) window-onload-fn))
   (add-watch page-state watch-key
              (fn [_ _ old new]
                (when (not= old new)
                  (let [old-value (get-in old value-path)
                        new-value (get-in new value-path)]
                    (when (not= old new)
                      (do-fn new-value))))))))

;; END page state
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Navigation and menu
;;* tag: <nav menu>
;;*
;;* description: Навигация и элементы меню
;;*
;;**************************************************************************************************

(defn navbar [body]
  [:ul {:class "nav navbar-nav navbar-right"} body])

(defn nav-menu-item
  ([title attrs] (nav-menu-item title attrs nil))
  ([title glyphicon-name attrs]
   [:li
    [:a (merge {:href "#"} attrs)
     (when glyphicon-name
       [:span {:class (str "glyphicon " glyphicon-name) :aria-hidden "true"}])
     " " title]]))

(defn nav-menu
  ([title attrs nav-menu-items] (nav-menu title nil attrs nav-menu-items))
  ([title glyphicon-name attrs nav-menu-items]
   [:li {:class "dropdown"}
    [:a {:class "dropdown-toggle" :aria-expanded "false", :role "button", :data-toggle "dropdown", :href "#"}
     (when glyphicon-name
       [:span {:class (str "glyphicon " glyphicon-name) :aria-hidden "true"}])
     " " title
     [:span {:class "caret"}]]
    [:ul {:class "dropdown-menu" :role "menu"}
     nav-menu-items
     ]]))

;; END Navigation and menu
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN component pagenator
;;* tag: <component pagenator>
;;*
;;* description: Переключатель страничного контента
;;*
;;**************************************************************************************************


(defn t-table-paginator-update-value-from-map [id {:keys [page page-size]}]
  (let [id (xml-id id)
        input-id   (xml-id :page-num id)
        select-id  (xml-id :btn-goto-next-page id)]
    (when page (dommy/set-value! (by-id input-id) page))
    (when page (dommy/set-value! (by-id select-id) page-size)) ))

(defn t-table-paginator [id class event-chan init-params]
  (let [id (xml-id id)
        input-id (xml-id :page-num id)]

    [:div {:id (name id) :style "display:inline-block;width:290px"}
     [:div {:style "display:table;width:auto"}
      [:div {:style "display:table-row;width:auto;clear:both;"}
       [:div {:style "float:left;display:table-column;width:75%"}
        [:div {:class "input-group"}

         [:span {:class "input-group-btn"}
          [:button {:id (name (xml-id :btn-goto-first-page id))
                    :class "btn btn-default"
                    :on-click (fn []
                                (dommy/set-value! (by-id input-id) 1)
                                (put! event-chan [id :page 1]))}
           [:span {:class "glyphicon glyphicon-fast-backward" :aria-hidden "true"}]]
          [:button {:id (name (xml-id :btn-goto-previus-page id))
                    :class "btn btn-default"
                    :on-click (fn []
                                (let [input (by-id input-id)
                                      v (dec (js/parseInt (dommy/value input)))]
                                  (when (< 0 v)
                                    (dommy/set-value! input v)
                                    (put! event-chan [id :page v]))))}
           [:span {:class "glyphicon glyphicon-step-backward" :aria-hidden "true"}]] ]

         [:input {:id (name input-id)
                  :class "form-control"
                  :style "text-align: center"
                  :type "number" :min "1"
                  :value (or (:page init-params) 1)
                  :placeholder "Страница"
                  :on-change #(this-as this (put! event-chan [id :page (js/parseInt (dommy/value this))]))
                  }]

         [:span {:class "input-group-btn"}

          [:button {:id (name (xml-id :btn-goto-next-page id))
                    :class "btn btn-default"
                    :on-click (fn []
                                (let [input (by-id input-id)
                                      v (inc (js/parseInt (dommy/value input)))]
                                  (when (< 0 v)
                                    (dommy/set-value! input v)
                                    (put! event-chan [id :page v]))))}
           [:span {:class "glyphicon glyphicon-step-forward" :aria-hidden "true"} ]]
          ]]]
       [:div {:style "float:left;display:table-column;width: 20%; margin-left: 4px"}
        [:select {:id (name (xml-id :select-page-size id))
                  ;;:style "float: left; width: 80px; border: 1px solid;"
                  :style "display: table-cell;"
                  :class "btn btn-default dropdown-toggle form-control"
                  :on-change #(this-as this (put! event-chan [id :page-size (js/parseInt (dommy/value this))]))}
         (let [selected-i (or (:page-size init-params) nil)]
           (map (fn [i] [:option (if (= i selected-i) {:value i :selected true} {:value i}) i])
                [5 10 15 20 50 100 1000]))
         ]]
       ]]]))


;; END component pagenator
;;..................................................................................................

;; -------------------------------------------------------

(defn clear-and-set [e body]
  (dommy/clear! e)
  (dommy/append! e (hipo/create body))
  e)

(defn clear-and-set-on-tag-by-id [id body]
  (clear-and-set (by-id id) body))

;;**************************************************************************************************
;;* BEGIN Tabs components
;;* tag: <html components tabs>
;;*
;;* description: табуляторы
;;*
;;**************************************************************************************************

(defn tab-entry [tab-label-id slave-ids]
  [tab-label-id slave-ids])

(defn make-tabs [page-state tab-value-path swith-tab-map]
  (add-watch-on-update-for
   page-state (->> tab-value-path
                   (reduce #(str %1 "-" (name %2)) "tab-element")
                   keyword)
   tab-value-path

   (fn []
     (doseq [[i [l _]] (seq swith-tab-map)]
       (dommy/listen! (sel1 l) :click #(swap! page-state assoc-in tab-value-path i))))

   (fn [tab-index]
     ;; deactivate/hide all
     (doseq [[l es] (vals swith-tab-map)]
       (do (dommy/remove-class! (sel1 l) :active)
           (doseq [e es]
             (dommy/hide! (sel1 e)))))

     (let [[l es] (swith-tab-map tab-index)]
       (when-let [l (sel1 l)]
         (dommy/add-class! l :active)
         (doseq [e es]
           (dommy/show! (sel1 e)))))
     )))

;; END tabs components
;;..................................................................................................




;;**************************************************************************************************
;;* BEGIN Modal dialogs
;;* tag: <modal dialog>
;;*
;;* description: Модальные диалоги
;;*
;;**************************************************************************************************

(defn modal-title [modal text]
  (dommy/set-text! (sel1 modal :.modal-title) text))

(defn modal-header [modal body]
  (clear-and-set (sel1 modal :.modal-header) body))

(defn modal-body [modal body]
  (clear-and-set (sel1 modal :.modal-body) body))

(defn modal-footer [modal body]
  (clear-and-set (sel1 modal :.modal-footer) body))

(defn modal-footer-div [modal body]
  (clear-and-set (-> modal (sel1 :.modal-footer) (sel1 :div)) body))

(defn modal-show [id]
  (println "Show modal on id = " id)
  (-> id name js/jQuery (.modal "show")))


;; ----------------------------------------------------------------

(def modal-dialog-id-suffix :modal-dialog)

(defn modal-cr-and-show-on-id [id {:keys [label header title body footer]}]
  (let [modal-id (xml-id modal-dialog-id-suffix id)]
    (clear-and-set-on-tag-by-id id
                                [:div {:id (name modal-id) :aria-hidden "true", :aria-labelledby (or label "Modal Label..."),
                                       :class "modal"
                                       :role "dialog", :tabindex "-1"}
                                 [:div {:class "modal-dialog"}
                                  [:div {:class "modal-content"}
                                   [:div {:class "modal-header"}
                                    (or header [:h4.modal-title (or title "Modal title...")])]
                                   [:div {:class "modal-body"} (or body nil)]
                                   [:div {:class "modal-footer"}
                                    [:div  {:class "navbar-left"}]
                                    (or footer [:button {:class "btn btn-default" :type "button" :data-dismiss "modal"} "Закрыть"])
                                    ]]]])
    (modal-show (str \# (name modal-id)))))

(defn modal-hide-on-id [id]
  (-> (xml-id modal-dialog-id-suffix id)
      name
      ((partial str \#))
      js/jQuery (.modal "hide")))



;; YES - NO - dialogs

(defn dialog-delete-row-yes?-no? [row modal-id fn-if-yes]
  (modal-cr-and-show-on-id
   modal-id
   {:title (str "Удаление записи #" (:id row))
    :body  [:div {:class "well center-block" :style "max-width:400px;"}
            "Желаете удалить запись?"]
    :footer (list
             [:button {:class "btn btn-danger btn-lg"
                       :type "button" :data-dismiss "modal"
                       :on-click fn-if-yes}
              "Удалить!"]
             [:button {:class "btn btn-lg btn-default" :data-dismiss "modal" :type "button"}
              "Закрыть"])
    }))



;; END Modal dialogs
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN Inputs validation
;;* tag: <input validation>
;;*
;;* description: Валидация элементов ввода
;;*
;;**************************************************************************************************

(defn input-validate-and-assoc [a k input-id validators]
  (let [input (by-id input-id)
        value (dommy/value input)
        parent (dommy/parent input)
        error-e-id (xml-id "-p-error" input-id)]

    (try
      (when-let [error-e (by-id error-e-id)]
        (dommy/remove! error-e))

      (doseq [validator validators]
        (validator input value))

      (dommy/remove-class! parent :has-error)

      (assoc a k value)

      (catch js/Error e
        (.log js/console (str "Ошибка валидации на: " input-id " валидатор выдал сообщение " e))
        (dommy/add-class! parent :has-error)
        (dommy/append! parent
                       (hipo/create [:p {:id (name error-e-id) :class "text-danger"} (str e)]))

        (assoc-in a [:error k] value)))))

(defn input-all-valid-or-nil [{e :error :as results}]
  (if (empty? e) results nil))


(defn validate-for-not-empty [_ value]
  (.log js/console (str "validate?: " value))
  (if (or (nil? value) (empty? value))
    (throw (js/Error. "пустое значение"))
    value))

(defn validate-equal-vals [message input-id-2 input-1 value-1]
  (let [input-2 (by-id input-id-2)
        value-2 (dommy/value input-2)]
    (if (= value-1 value-2)
      value-1
      (throw (js/Error. message)))))


;; END Inputs validation
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Form engine
;;* tag: <form tngine>
;;*
;;* description: Функционал работы с формами
;;*
;;**************************************************************************************************



;; END Form engine
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN FTS
;;* tag: <fts>
;;*
;;* description: для полнотекстового поиска
;;*
;;**************************************************************************************************


(defn input-fts [{:keys [id
                         value
                         on-change-fn
                         find-fn]
                  :or {id :fts-form-div
                       value ""
                       on-change-fn #(println "Функция изменения значения поискового поля не задана! " %)
                       find-fn #(println "Функция выполнения поискового запроса не задана!")}}]
  (let [input-id (xml-id :input id)]
    [:div {:id id :class "input-group"}
     [:span {:class "input-group-btn"}
      [:button {:class "btn btn-default" :type "button"

                :on-click #(do
                             (dommy/set-value! (by-id input-id) "")
                             (on-change-fn "")
                             (find-fn))

                } [:span {:class "glyphicon glyphicon-remove" :aria-hidden "true"}]]
      ]
     [:input {:id (name input-id)
              :type "text" :class "form-control" :placeholder "Искать по..."
              :on-change #(this-as this (on-change-fn (dommy/value this)))
              :on-keypress #(this-as this
                                     (do
                                       (on-change-fn (dommy/value this))
                                       (when (= 13 (.-keyCode %)) (find-fn))))
              :value value
              }]
     [:span {:class "input-group-btn"}
      [:button {:class "btn btn-default" :type "button"
                :on-click find-fn
                } [:span {:class "glyphicon glyphicon-search" :aria-hidden "true"}]]
      ]
     ]
    ))

;; END FTS
;;..................................................................................................
;;**************************************************************************************************
;;* BEGIN ajax
;;* tag: <ajax tools>
;;*
;;* description: Инструментарий для работы с ajax
;;*
;;**************************************************************************************************




(def chan-ajax-post-json (chan))
(def chan-ajax-post-json-white-end (chan))

(go
  (while true
    (let [[url params handler-fn error-handler-fn] (<! chan-ajax-post-json)]
      (println "AJAX POST REQUEST: START ON " url " <- " params)


      (let [el [:div {:id "top-download-message" :class "alert alert-warning" :style "position:absolute;top:40%;left:40%" :role "alert"}
                [:img {:src "/images/preloader.gif"}]
                "Подождите, идет загрузка данных..."]]
        (->> el
             hipo/create
             (dommy/append! (sel1 :body))))

      ;; Данный вариант не работает в общей функции
      ;; #_(POST "/tag/path-and-chailds"
      ;;         {:params params
      ;;          :error-handler error-handler-fn
      ;;          :format :json
      ;;          :response-format :json
      ;;          :keywords? true
      ;;          :handler (fn [response]
      ;;                     (handler-fn response)
      ;;                     (println "AJAX POST REQUEST: END ON " url))
      ;;         })

      (ajax.core/ajax-request
       {:uri url
        :params params
        :method :post
        ;;:timeout 20000
        :format (ajax.core/json-request-format)
        :response-format (ajax.core/json-response-format {:keywords? true})
        :handler (fn [[ok response]]

                   (put! chan-ajax-post-json-white-end 1)

                   (if ok
                     (handler-fn response)
                     (do (println "AJAX ERROR: " ok)
                         (error-handler-fn response)))

                   ;;(println "AJAX POST RESPONSE ON " url " ->  " response)

                   (println "AJAX POST REQUEST: END ON " url))
        })

      (let [_ (<! chan-ajax-post-json-white-end)]
        (when-let [e (by-id :top-download-message)] (dommy/remove! e))))))

(defn ajax-post-json
  ([url params handler-fn]
   (ajax-post-json url params handler-fn error-handler))
  ([url params handler-fn error-handler-fn]
   (put! chan-ajax-post-json [url params handler-fn error-handler-fn])
   ))

;; END ajax
;;..................................................................................................
