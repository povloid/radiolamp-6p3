(ns ixinfestor.omut

  (:require-macros [cljs.core.async.macros :refer [go]])

  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]

            [ixinfestor.io :as ix-io]
            [ixinfestor.net :as ixnet]

            [clojure.set :as clojset]
            [clojure.string :as clojstr]

            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]

            [goog.string :as gstring]
            [goog.string.format]
            )

  (:import [goog.dom query]))

;;**************************************************************************************************
;;* BEGIN Common functions and tools
;;* tag: <com>
;;*
;;* description: Общие функции и инструменты
;;*
;;**************************************************************************************************

(defn by-id
  "Short-hand for document.getElementById(id)"
  [id]
  (.getElementById js/document (name id)))

(defn uniq-id [s]
  (str (gensym (str s "-"))))


(defn is-numeric? [n]
  (and (not (js/isNaN (js/parseFloat n))) (js/isFinite n)))



(defn parse-int-or [v dv]
  (if (is-numeric? v)
    (js/parseInt v)
    dv))

(defn parse-int-or-nil [v]
  (parse-int-or v nil))

(defn parse-int-or-zero [v]
  (parse-int-or v 0))


(defn parse-float-or [v dv]
  (if (is-numeric? v)
    (js/parseFloat v)
    dv))

(defn parse-float-or-nil [v]
  (parse-float-or v nil))

(defn parse-float-or-zero [v]
  (parse-float-or v 0))



(defn parse-number-or [v dv]
  (if (is-numeric? v)
    (new js/Number v)
    dv))

(defn parse-number-or-nil [v]
  (parse-number-or v nil))

(defn parse-number-or-zero [v]
  (parse-number-or v 0))







(defn on-click-com-fn [f]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (f)
    1))



;; Работа с фалами и расширениями

(defn file-ext [filename]
  (let [m (js->clj (.split filename "."))]
    (when (< 1 (count m)) (last m))))

(defn file-type [filename]
  (let [ext (file-ext filename)]
    (assoc (condp = ext
             nil   {:type :dir  :title "Папка"}
             "jpg" {:type :file :title "Фаил изображения"}
             {:type :file :title "Фаил"})
           :ext ext)))



(defn path-up [path]
  (let [p (clojure.string/split path "/")
        c (count p)]
    ;;(println p c)
    (if (< c 3) "/"
        (->> p
             reverse
             rest
             reverse
             (reduce (fn [a s]
                       (if (or (empty? s) (= s "/")) a
                           (str a "/" s)))
                     "")))))

;; END Common functions and tools
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN omut row spec
;;* tag: <omut row spec>
;;*
;;* description: Данные для отображения строк
;;*
;;**************************************************************************************************


(def omut-row-key :omut-row)

(def omut-row-app-state {:selected false :collapsed {}})


(defn omut-row-init [row]
  (assoc row omut-row-key omut-row-app-state))

(defn omut-row-if-not-init-init!! [app]
  (om/transact!
   app
   (fn [app]
     (if (omut-row-key app)
       app
       (omut-row-init app)))))

(defn omut-row-row [row]
  (dissoc row omut-row-key))



(defn omut-row-set-selected! [app-row v]
  (assoc-in app-row [omut-row-key :selected] v))

(defn omut-row-set-selected!! [app-row v]
  (om/update! app-row [omut-row-key :selected] v))

(defn omut-row-set-selected-not!! [app-row]
  (om/transact! app-row [omut-row-key :selected] not))

(defn omut-row-selected? [app-row]
  (get-in app-row [omut-row-key :selected] false))





(defn omut-row-set-collapsed!! [app-row k v]
  (om/transact!
   app-row
   (fn [app-row]
     (assoc-in app-row [omut-row-key :collapsed k] v))))

(defn omut-row-set-collapsed-not!! [app-row k]
  (om/transact!
   app-row
   (fn [app-row]
     ;;(println (app-row omut-row-key))
     (let [v (get-in app-row [omut-row-key :collapsed k])]
       (assoc-in app-row [omut-row-key :collapsed k] (if (nil? v) false (not v)))))))

(defn omut-row-collapsed? [app-row k]
  (get-in app-row [omut-row-key :collapsed k] true))



;; END omut-row
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Dates and Times
;;* tag: <date time timestamp>
;;*
;;* description: функции работы со временем
;;*
;;**************************************************************************************************

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
    {:FULL_DATE       (.-FULL_DATE f)
     :FULL_DATETIME   (.-FULL_DATETIME f)
     :FULL_TIME       (.-FULL_TIME f)
     :LONG_DATE       (.-LONG_DATE f)
     :LONG_DATETIME   (.-LONG_DATETIME f)
     :LONG_TIME       (.-LONG_TIME f)
     :MEDIUM_DATE     (.-MEDIUM_DATE f)
     :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
     :MEDIUM_TIME     (.-MEDIUM_TIME f)
     :SHORT_DATE      (.-SHORT_DATE f)
     :SHORT_DATETIME  (.-SHORT_DATETIME f)
     :SHORT_TIME      (.-SHORT_TIME f)
     }))


(defn str-to-date [date-string]
  (let [d (new js/Date (.parse js/Date date-string))]
    (if (js/isNaN d) nil d)))


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
  (if (nil? date) nil
      (.format (goog.i18n.DateTimeFormat. (or (date-formats date-format) date-format))  date)))

(defn str-to-date-and-format [date-format alt-string s]
  (if (nil? s) alt-string
      (format-date date-format (str-to-date s))))


(defn date-com-format-date [d]
  (format-date "yyyy-MM-dd" d))

(defn date-com-format-datetime [d]
  (format-date "yyyy-MM-dd HH:mm:ss z" d))

(defn date-com-format-datetime-to-min [d]
  (format-date "yyyy-MM-dd HH:mm z" d))

(defn the-time-has-passed-from [date-1 date-2]
  (let [time-diff (.abs js/Math
                        (- (.getTime date-2)
                           (.getTime date-1)))

        diff-ds   (.floor js/Math
                          (/ time-diff
                             (* 1000 60 60 24)))

        diff-hs   (.floor js/Math
                          (/ time-diff
                             (* 1000 60 60)))

        diff-ms   (.floor js/Math
                          (/ time-diff
                             (* 1000 60)))]
    [
     ;; Дни
     diff-ds
     ;; Часы
     (- diff-hs (* diff-ds 24))
     ;; Минуты
     (- diff-ms (* diff-hs 60))
     ]))

(defn the-time-has-passed-from-the-date [date]
  (the-time-has-passed-from date (new js/Date)))

(defn the-time-has-passed-from-the-date-as-str [date]
  (let [[d h m] (the-time-has-passed-from-the-date date)]
    (gstring/format "прошло: %d дн. %02d час. %02d мин." d h m)))


(declare ui-glyphicon)


(defn ui-datetime [d]
  (dom/span
   #js {:className "text-muted"}
   (ui-glyphicon "time") " "
   (date-com-format-datetime d)))




(defn ui-cdate-udate [{:keys [cdate udate]}]
  (dom/span
   #js {:className "text-muted"}
   (ui-glyphicon "time") " создан "
   (date-com-format-datetime cdate) " "
   (ui-glyphicon "time") " обновлен "
   (date-com-format-datetime udate)))





;; END date and time functions
;;..................................................................................................



;; END Dates and Times
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN bootstrap-containers
;;* tag: <layout containers>
;;*
;;* description: Контенеры
;;*
;;**************************************************************************************************

(defn ui-container [& body]
  (apply dom/div #js {:className "container"} body))

(defn ui-container-fluid [& body]
  (apply dom/div #js {:className "container-fluid"} body))

(defn ui-row [& body]
  (apply dom/div #js {:className "row"} body))



;; END bootstrap-containers
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN icons
;;* tag: <icons>
;;*
;;* description: Иконки
;;*
;;**************************************************************************************************

(defn ui-glyphicon [name & [class+ size]]
  (dom/span #js {:className   (str "glyphicon glyphicon-"
                                   name " " (or class+ ""))
                 :style       #js {:fontSize size}
                 :aria-hidden "true"}))

;; END icons
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN buttons
;;* tag: <buttons>
;;*
;;* description: Кнопки
;;*
;;**************************************************************************************************

(defn ui-button [{:keys [text
                         type
                         size
                         block?
                         disabled?
                         active?
                         on-click
                         style]
                  :or   {text "Кнопка"
                         type :default}}]
  (dom/button #js {:className  (str "btn "
                                    ({:default "btn-default"
                                      :primary "btn-primary"
                                      :success "btn-success"
                                      :info    "btn-info"
                                      :warning "btn-warning"
                                      :danger  "btn-danger"
                                      :link    "btn-link"} type)
                                    (if size ({:lg " btn-lg"
                                               :sm " btn-sm"
                                               :xs " btn-xs"} size)
                                        "")
                                    (if block? " btn-block" "")
                                    (if active? " active" "")
                                    )
                   :type       "button"
                   :disabled   (if disabled? "disabled" "")
                   :onClick    (on-click-com-fn on-click)
                   :onTouchEnd (on-click-com-fn on-click)
                   :style      style
                   }
              text))


;; END buttons
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN modal
;;* tag: <modal>
;;*
;;* description: Модальные диалоги
;;*
;;**************************************************************************************************

(def modal-app-init
  {:show false})

(defn modal-show [app]
  (om/transact!
   app :show
   (fn [i] (if i (inc i) 1))))

(defn modal-hide [app]
  (om/transact!
   app :show
   (fn [_] false)))


(defonce modals-status (atom #{}))
(add-watch
 modals-status :log
 (fn [_ _ old new]
   (when (not= old new)
     (println "modals-status: " new)
     (let[tag-body (aget (query "body") 0)]
       (if (empty? new)
         (gdc/remove tag-body "modal-open")
         (gdc/add    tag-body "modal-open")))

     (let [d (clojure.set/difference new old)]
       (when (not (empty? d))
         (let [new-dialog-id (first d)]
           (println "Открылся " new-dialog-id)))))))

(defn modal [app owner {:keys [label
                               modal-size
                               header
                               body
                               footer
                               class+
                               on-close-button-fn]
                        :or   {label      "Пустая пометка"
                               modal-size :default
                               class+     "" }
                        }]
  (reify
    om/IInitState
    (init-state [_]
      {:id (uniq-id "modal")})
    om/IWillUnmount
    (will-unmount [_]
      (swap! modals-status disj (om/get-state owner :id)))
    ;; om/IDidUpdate
    ;; (did-update [_ next-props _]
    ;;   (println "IDidUpdate" (om/get-state owner :id) (:show next-props) "->" (:show app))
    ;;   (when (< 1 (count @modals-status))
    ;;     (let [id (om/get-state owner :id)
    ;;           e (.getElementById js/document id)
    ;;           scrollTop (.-scrollTop e)
    ;;           itop (.-innerWidth js/top)
    ;;           ]
    ;;       ;;(.setAttribute e "style" (str "margin-top:"  (.-scrollTop e) "px"))
    ;;       ))
    ;;   )
    om/IWillUpdate
    (will-update [_ next-props _]
      ;;(println "IWillUpdate" (:show next-props) "->" (:show app))
      (when (not (:show next-props))
        (set! (.-scrollTop (.getElementById js/document (om/get-state owner :id))) 0)))
    om/IRenderState
    (render-state [this {id :id}]
      ;;(println "modal id:" id)
      (let [show? (:show app)]
        (swap! modals-status (if show? conj disj) id)
        (dom/div #js {:id              id
                      :aria-hidden     "true"
                      :aria-labelledby label
                      :style           (if show?
                                         #js {:display         "block"
                                              :paddingLeft     0
                                              :backgroundColor "rgba(0, 0, 0, 0.2)"}
                                         #js {:display "none" })
                      :className       (if show? "modal in" "modal")
                      :role            "dialog"
                      :tabIndex        "-1"}
                 (dom/div #js {:className (str "modal-dialog"
                                               (condp = modal-size
                                                 :sm " modal-sm"
                                                 :lg " modal-lg"
                                                 "")
                                               " " class+)}
                          (dom/div #js {:className "modal-content"}
                                   (dom/div #js {:className "modal-header"}
                                            (dom/button #js {:type         "button" :className  "close"
                                                             :data-dismiss "modal"  :aria-label "Close"
                                                             :onClick      (fn [_]
                                                                             (when on-close-button-fn
                                                                               (on-close-button-fn))
                                                                             (om/update! app :show false) 1)}
                                                        (ui-glyphicon "remove"))

                                            (or header (dom/h4 #js {:className "modal-title"} label)))
                                   (dom/div #js {:className "modal-body"
                                                 :style     #js {:marginRight 40}}
                                            (or body (dom/p #js {:className "text-info"}
                                                            "Пустое пространство диалога. Можно наполнить элементами")))
                                   (dom/div #js {:className "modal-footer"}
                                            (or footer (ui-button {:type     :default
                                                                   :on-click (fn [_]
                                                                               (when on-close-button-fn
                                                                                 (on-close-button-fn))
                                                                               (om/update! app :show false)
                                                                               1)
                                                                   :text     "Закрыть"}))))))))))


;;------------------------------------------------------------------------------
;; BEGIN: buttons modal for select actions variants
;; tag: <buttons modal for select actions variants>
;; description: Диалог для выбора вариантов действия
;;------------------------------------------------------------------------------

(defn actions-modal-button [app _ {:keys [text
                                          btn-type
                                          act-fn]
                                   :or   {text     "Метка события"
                                          btn-type :default}}]
  (reify
    om/IRender
    (render [_]
      (ui-button {:text     text
                  :type     btn-type
                  :block?   true
                  :size     :lg
                  :on-click (fn [_]
                              (modal-hide app)
                              (if act-fn
                                (act-fn)
                                (println "Действие для '" text "' еще не определено"))
                              1)
                  }))))




(def actions-modal-app-init modal-app-init)


(defn actions-modal [app owner {:keys [chan-open]}]
  (reify
    om/IInitState
    (init-state [_]
      {:actions
       {:label "Пусто"
        :acts  []}})
    om/IWillMount
    (will-mount [this]
      (when chan-open
        (go
          (while true
            (let [actions (<! chan-open)]
              (om/set-state! owner :actions actions)
              (modal-show app))))))
    om/IRenderState
    (render-state [_ {{:keys [label acts]} :actions}]
      (om/build modal app
                {:opts {:label      label
                        :modal-size :sm
                        :body       (let [buttons (map
                                                   (fn [opts]
                                                     (om/build actions-modal-button app {:opts opts}))
                                                   acts)]
                                      (if (empty? buttons)
                                        (dom/h2 nil "Действий нет")
                                        (apply dom/div nil buttons)))
                        }}
                ))))

;; END buttons modal for select actions variants
;;..............................................................................

;;------------------------------------------------------------------------------
;; BEGIN: Modal Yes or No
;; tag: <modal yes no yn>
;; description: Диалог зароса действия да или нет
;;------------------------------------------------------------------------------

(def modal-yes-no-app-init modal-app-init)

(defn modal-yes-no [app owner {:keys [act-yes-fn]
                               :or   {act-yes-fn #(js/alert "Действие еще не реализовано")}
                               :as   opts}]
  (reify
    om/IRender
    (render [_]
      (om/build modal app
                {:opts (assoc opts :footer
                              (dom/div nil

                                       (ui-button {:type     :primary
                                                   :on-click (fn [_]
                                                               (act-yes-fn)
                                                               (modal-hide app) 1)
                                                   :text     "Да"})

                                       (ui-button {:on-click (fn [_] (modal-hide app) 1)
                                                   :text     "Нет"})

                                       ))
                 } ))))

;; END Modal Yes or No
;;..............................................................................

;;------------------------------------------------------------------------------
;; BEGIN: Function for showing modal for error or warning mesage
;; tag: <modal error warning message>
;; description: Диалог для выведения сообщений об ошибках
;;------------------------------------------------------------------------------

(defonce message-modal-app
  (atom (assoc modal-app-init
               :title
               :message "")))

(defn- cursor-message-root [] (om/root-cursor message-modal-app))

(def message-modal-id "message-modal")

(defn message-modal [app _]
  (reify
    om/IRender
    (render [_]
      (let [[type-class+ title-default icon]
            (get-in {:muted   ["text-muted"   "Текстовое сообщение"
                               nil]
                     :primary ["text-primary" "Предложение"
                               nil]
                     :success ["text-success" "Операция проведени успешно"
                               "ok"]
                     :info    ["text-info"    "Информация"
                               "info"]
                     :warning ["text-warning" "Внимание!"
                               "alert"]
                     :danger  ["text-danger"  "Ошибка!!!"
                               "exclamation-sign"]
                     } [(get-in @app [:type] :muted)])]

        (om/build modal app
                  {:opts {:header (dom/h1 #js {:className type-class+}
                                          (when icon (ui-glyphicon icon "1em"))
                                          (or (@app :title) title-default))
                          :body   (dom/p #js {:className type-class+}
                                         " " (@app :message))}})))))


(defn show-in-message-modal [type {:keys [title message] :as message-row}]
  (let [_ (or (by-id message-modal-id)
              (let [error-div (.createElement js/document "div")
                    tag-body  (aget (query "body") 0)]
                (set! (.-id error-div) message-modal-id)
                (.appendChild tag-body error-div)

                (om/root message-modal
                         message-modal-app
                         {:target (by-id message-modal-id)})

                error-div))]

    (om/transact! (cursor-message-root)
                  (fn [app]
                    (assoc app
                           :title title
                           :message (or message (str message-row))
                           :type type)))
    (modal-show (cursor-message-root))))

(def show-in-message-modal-muted   (partial show-in-message-modal :muted))
(def show-in-message-modal-primary (partial show-in-message-modal :primary))
(def show-in-message-modal-success (partial show-in-message-modal :success))
(def show-in-message-modal-info    (partial show-in-message-modal :info))
(def show-in-message-modal-warning (partial show-in-message-modal :warning))
(def show-in-message-modal-danger  (partial show-in-message-modal :danger))


;; END Function for showing modal for error or warning mesage
;;..............................................................................


;; END modal
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN Long text collapser
;;* tag: <long text collapser>
;;*
;;* description: Компонент для отображение длянного текста
;;*
;;**************************************************************************************************


(defn text-collapser [app _ {k :k}]
  (reify
    om/IWillMount
    (will-mount [_]
      (omut-row-if-not-init-init!! app))
    om/IRender
    (render [_]
      (let [text       (app k "")
            text-count (count text)]
        (dom/p nil
               (ui-button {:text     "..."
                           :size     :xs
                           :active?  (not (omut-row-collapsed? @app k))
                           :on-click #(omut-row-set-collapsed-not!! app k)})

               (if (and (omut-row-collapsed? @app k) (> text-count 90))
                 (str (.substring text 0 89) "...")
                 text))))))

;; END Long text collapser
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN collapser
;;* tag: <collapser>
;;*
;;* description: Свертыватель
;;*
;;**************************************************************************************************

                                        ;TODO: Перемиеноывать в ui-hidder
(defn ui-collapser [app text k show? collapsed-body]
  (dom/p #js {:style #js {:display (if show? "" "none")}}
         (ui-button {:text     text
                     :type     :info
                     :active?  (not (omut-row-collapsed? @app k))
                     :on-click #(omut-row-set-collapsed-not!! app k)})

         (dom/div #js {:style #js {:display (if (omut-row-collapsed? @app k) "none" "")}}
                  collapsed-body)))




;; END collapser
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN label
;;* tag: <label>
;;*
;;* description: Пометки
;;*
;;**************************************************************************************************

(defn ui-label [type text]
  (dom/span #js {:className (str  "label label-"
                                  (get {:default "default"
                                        :primary "primary"
                                        :success "success"
                                        :info    "info"
                                        :warning "warning"
                                        :danger  "danger"
                                        } type "default"))
                 :style     #js {:whiteSpace "normal"}} text))

;; END label
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN helper
;;* tag: <helper>
;;*
;;* description: Текстовые подсказки
;;*
;;**************************************************************************************************

(def helper-p-app-init
  {:text-muted   nil
   :text-primary nil
   :text-success nil
   :text-info    nil
   :text-warning nil
   :text-danger  nil})


(defn helper-p [app _]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [text-muted
                    text-primary
                    text-success
                    text-info
                    text-warning
                    text-danger]} @app]
        (dom/div nil
                 (when text-muted
                   (dom/p #js {:className "text-muted"} text-muted))
                 (when text-primary
                   (dom/p #js {:className "text-primary"} text-primary))
                 (when text-success
                   (dom/p #js {:className "text-success"} text-success))
                 (when text-info
                   (dom/p #js {:className "text-info"} text-info))
                 (when text-warning
                   (dom/p #js {:className "text-warning"} text-warning))
                 (when text-danger
                   (dom/p #js {:className "text-danger"} text-danger))  )))))


(defn helper-p-clean [app]
  (om/transact! app
                (fn [app]
                  (dissoc app
                          :text-muted
                          :text-primary
                          :text-success
                          :text-info
                          :text-warning
                          :text-danger))))

(defn helper-p-clean-and-set!! [app k message]
  (om/transact! app (fn [app]
                      (-> app
                          (dissoc :text-muted
                                  :text-primary
                                  :text-success
                                  :text-info
                                  :text-warning
                                  :text-danger)
                          (assoc k message)))))


;; END helper
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN alert
;;* tag: <alert>
;;*
;;* description: Текстовые подсказки в цветных блоках
;;*
;;**************************************************************************************************

(def alert-app-init
  {:alert-muted   nil
   :alert-primary nil
   :alert-success nil
   :alert-info    nil
   :alert-warning nil
   :alert-danger  nil})


(defn alert [app _]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [alert-muted
                    alert-primary
                    alert-success
                    alert-info
                    alert-warning
                    alert-danger]} app]
        (dom/div nil
                 (when alert-muted
                   (dom/div #js {:className "alert alert-muted"} alert-muted))
                 (when alert-primary
                   (dom/div #js {:className "alert alert-primary"} alert-primary))
                 (when alert-success
                   (dom/div #js {:className "alert alert-success"} alert-success))
                 (when alert-info
                   (dom/div #js {:className "alert alert-info"} alert-info))
                 (when alert-warning
                   (dom/div #js {:className "alert alert-warning"} alert-warning))
                 (when alert-danger
                   (dom/div #js {:className "alert alert-danger"} alert-danger))  )))))


(defn alert-clean [app]
  (om/transact! app
                (fn [app]
                  (dissoc app
                          :alert-muted
                          :alert-primary
                          :alert-success
                          :alert-info
                          :alert-warning
                          :alert-danger))))

(defn alert-clean-and-set!! [app k message]
  (om/transact! app
                (fn [app]
                  (-> app
                      (dissoc :alert-muted
                              :alert-primary
                              :alert-success
                              :alert-info
                              :alert-warning
                              :alert-danger)
                      (assoc k message)))))


;; END alert
;;..................................................................................................







;;**************************************************************************************************
;;* BEGIN has
;;* tag: <has>
;;*
;;* description: Подсветка элементов
;;*
;;**************************************************************************************************

(def input-css-string-has?-app-init
  {:has-success? nil
   :has-warning? nil
   :has-error?   nil})


(defn input-css-string-has? [{:keys [has-success?
                                     has-warning?
                                     has-error?]}]
  (condp = true
    has-error?   "has-error"
    has-warning? "has-warning"
    has-success? "has-success"
    ""))

(defn input-css-string-has?-clean [app]
  (om/transact! app
                (fn [app]
                  (dissoc app
                          :has-success?
                          :has-warning?
                          :has-error?))))

(defn input-css-string-has?-clean-and-set! [app k]
  (-> app
      (dissoc :has-success?
              :has-warning?
              :has-error?)
      (assoc k true)))

(defn input-css-string-has?-clean-and-set!! [app k]
  (om/transact! app
                (fn [app]
                  (input-css-string-has?-clean-and-set! app k))))

;; END has
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Throw validators
;;* tag: <throw validators>
;;*
;;* description: Валидаторы с исключениями и подсветкой
;;*
;;**************************************************************************************************

(defn get-valid-value-or-throw
  [app path get-val-fn pred? throw-message]
  (let [v (get-val-fn (get-in @app path))]
    (if (pred? v)
      (do (om/transact! app path #(assoc % :has-danger? true :text-danger throw-message))
          (throw (js/Error. throw-message)))
      v)))

;; END
;;..................................................................................................



(defn ui-form-group [{:keys [label label-class+ input-class+ body]
                      :or   {label        "метка"
                             label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                             input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"}}]
  (dom/div #js {:className "form-group"}
           (dom/label #js {:className (str "control-label " label-class+) } label)
           (dom/div #js {:className input-class+ :style #js {}}
                    body)))



;;**************************************************************************************************
;;* BEGIN input
;;* tag: <input>
;;*
;;* description: поле ввода
;;*
;;**************************************************************************************************


;; Пример от Девида Нолена, полезен для ввода чиселj...
;; (defn input-0 [app owner]
;;   (reify
;;     om/IInitState
;;     (init-state [_]
;;       {:value "" :count 0})
;;     om/IRenderState
;;     (render-state [_ {:keys [value]}]
;;       (dom/div nil
;;                (dom/label nil "Only numeric : ")
;;                (dom/input #js
;;                           {:value value
;;                            :onChange
;;                            #(let [new-value (-> % .-target .-value)]
;;                               (if (js/isNaN new-value)
;;                                 (om/set-state! owner :value value)
;;                                 (om/set-state! owner :value new-value)))})))))


(def input-app-init
  {:value ""})

(defn input-value [app] (get app :value))

(defn input [app owner {:keys [class+
                               type
                               onChange-valid?-fn
                               onChange-updated-fn
                               onKeyPress-fn
                               placeholder
                               min max step]
                        :or   {class+             ""
                               type               "text"
                               onChange-valid?-fn (fn [_ _] true)
                               onKeyPress-fn      (fn [_] nil)
                               placeholder        ""
                               }}]
  (reify
    om/IRender
    (render [this]
      (let [value (or (:value @app) "")]
        (dom/input #js {:value       value
                        :onChange    (fn [e]
                                       (let [new-value (.. e -target -value)]
                                         (if (onChange-valid?-fn app new-value)
                                           (om/update! app :value new-value)
                                           (om/update! app :value value))
                                         (when onChange-updated-fn
                                           (onChange-updated-fn))
                                         ))
                        :onKeyPress  onKeyPress-fn
                        :type        type
                        :min         min :max max :step step
                        :placeholder placeholder
                        :className   (str "form-control " class+)})))))

(defn input-form-group  [app owner {:keys [label
                                           type
                                           label-class+
                                           input-class+
                                           spec-input]
                                    :or   {label        "Метка"
                                           label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                           input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                           spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build input app {:opts spec-input})
                        (om/build helper-p app {})
                        )))))




(defn input-vldfn-not-empty [app v]
  (helper-p-clean app)
  (input-css-string-has?-clean app)
  (when (= (count (.trim v)) 0)
    (om/transact! app #(assoc % :has-warning? true :text-warning "Пустое поле")))
  true)


(defn input-vldfn-not-empty-or-0 [app v]
  (helper-p-clean app)
  (input-css-string-has?-clean app)
  (when (or (= (count (.trim v)) 0) (= (.valueOf (new js/Number v)) 0))
    (om/transact! app #(assoc % :has-warning? true :text-warning "Показание пустое либо равно нулю")))
  (do
    (om/transact! app #(assoc % :has-success? true))
    true))

(defn input-vldfn-not-empty-date [app v]
  (helper-p-clean app)
  (input-css-string-has?-clean app)
  (when-not (str-to-date v)
    (om/transact! app #(assoc % :has-warning? true :text-warning "Неправильная дата")))
  true)








(def input-datetime--date-str-format "yyyy-MM-ddTHH:mmZ")

(defn input-datetime-form-group--set-date! [app d]
  (assoc app :value (format-date input-datetime--date-str-format d)))

(defn input-datetime--parse-str-to-date [s]
  (let [parser (new goog.i18n.DateTimeParse input-datetime--date-str-format)
        d (new js/Date)]
    (.parse parser s d)
    d))


(defn input-datetime-form-group--date [app]
  (input-datetime--parse-str-to-date (app :value)))


(defn input-vldfn-not-empty-datetime [app v]
  (helper-p-clean app)
  (input-css-string-has?-clean app)
  (when-not (input-datetime--parse-str-to-date v)
    (om/transact! app #(assoc % :has-warning? true :text-warning "Неправильная дата")))
  true)




(defn input-datetime-form-group  [app owner {:keys [label
                                                    type
                                                    label-class+
                                                    input-class+
                                                    spec-input]
                                             :or   {label        "Метка"
                                                    label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                                    input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                                    spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (dom/b nil "введено: "
                               (date-com-format-datetime-to-min (input-datetime--parse-str-to-date (@app :value))))
                        (om/build
                         input app
                         {:opts (assoc spec-input
                                       :type                "datetime"
                                       :placeholder         input-datetime--date-str-format
                                       :onChange-valid?-fn  input-vldfn-not-empty-datetime)})
                        (om/build helper-p app {}))))))







;; END input
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN input select
;;* tag: <input select>
;;*
;;* description: Выбор из списка
;;*
;;**************************************************************************************************

(def no-select-v "NO-SELECT")

(def select-app-init
  {:selected no-select-v
   :list     []})

(defn select-app-list [app]
  (app :list))

(defn select-app-list-set! [app new-list]
  (assoc app :list new-list))

(defn select-app-selected-set! [app selected]
  (assoc app :selected (str selected)))

(defn select-app-selected-set-nil! [app]
  (assoc app :selected no-select-v))

(defn select-app-selected [app]
  (let [sv (app :selected)]
    (if (or (nil? sv)
            (empty? sv)
            (= sv no-select-v)) nil sv)))


(defn select-app-selected-int-or-nil [app]
  (when-let [v (select-app-selected app)]
    (js/parseInt v)))


(defn select [app _ {:keys [first-item-text
                            on-change-fn
                            value-field-key
                            disabled?
                            alert-warn-on-not-selected?
                            title-field-key]
                     :or   {value-field-key :id
                            title-field-key :keyname
                            first-item-text "Выбрать..."}}]
  (reify
    om/IRender
    (render [_]
      ;;(println "SELECT APP:" @app)
      (apply
       dom/select
       #js {:value     (@app :selected)
            :className "form-control"
            :disabled  (when disabled? "disabled")
            :onChange
            (fn [e]
              (let [v (-> e .-target .-value)]
                ;; Для подсветки при пустом значении
                (if (and alert-warn-on-not-selected?
                         (or (nil? v)
                             (= v no-select-v)
                             (empty? v)))
                  (om/transact! app #(assoc % :has-warning? true :text-warning "Невыбрано значение"))
                  (do (helper-p-clean app) (input-css-string-has?-clean app)))
                ;;Дальнейшая отработка действия
                (om/update! app :selected v)
                (when on-change-fn (on-change-fn v))))}

       (doall
        (map (fn [row]
               (dom/option #js {:value (str (value-field-key row))} (title-field-key row)))
             (into [{value-field-key no-select-v title-field-key first-item-text}] (@app :list)))) ))))

(defn select-form-group  [app _ {:keys [label
                                        type
                                        label-class+
                                        input-class+
                                        spec-select]
                                 :or   {label        "Метка"
                                        label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                        input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                        spec-select  {}}}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build select app {:opts spec-select})
                        (om/build helper-p app {})
                        )))))


;; select in list from url

(def select-from-url-app-init select-app-init)

(defn select-from-url [app owner {:keys [url params] :as select-opts}]
  (reify
    om/IWillMount
    (will-mount [_]
      (ixnet/get-data
       url params
       (fn [result]
         ;;(println result " to " @app)
         (om/update! app :list result))))
    om/IRender
    (render [_]
      (om/build select app {:opts select-opts}))))



(defn select-from-url-form-group  [app _ {:keys [label
                                                 type
                                                 label-class+
                                                 input-class+
                                                 spec-select]
                                          :or   {label        "Метка"
                                                 label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                                 input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                                 spec-select  {}}}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build select-from-url app {:opts spec-select})
                        (om/build helper-p app {})
                        )))))






;; END input select
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN Input change password
;;* tag: <input change password>
;;*
;;* description: Элемент ввода пароля
;;*
;;**************************************************************************************************

(def input-change-password-app-init
  {:password-1 {:value ""} :password-2 {:value ""} })

(defn input-change-password-clean [_]
  {:password-1 {:value ""} :password-2 {:value ""} })

(defn input-change-password-valid? [a]
  (= (get-in a [:password-1 :value]) (get-in a [:password-2 :value])))

(defn input-change-password-check [a]
  (if (not (input-change-password-valid? a))
    (throw (js/Error. "Пароли в полях не совпадают!"))
    a))

(defn input-change-password-value [a]
  (get-in a [:password-1 :value]))

(defn input-change-password [app owner]
  (letfn [(onChange-updated-fn []
            (om/transact!
             app (fn [app]
                   (if (not (input-change-password-valid? app))
                     (assoc app :has-warning? true :text-warning "Пароли не совпадают")
                     (dissoc app :text-warning :has-warning?))))
            )]
    (reify
      om/IRender
      (render [this]
        (dom/div
         nil

         (om/build input (:password-1 app) {:opts {:type                "password"
                                                   :onChange-updated-fn onChange-updated-fn}})
         (om/build input (:password-2 app) {:opts {:type                "password"
                                                   :onChange-updated-fn onChange-updated-fn}})
         (om/build helper-p app {})

         )))))

(defn input-change-password-group  [app owner {:keys [label
                                                      type
                                                      label-class+
                                                      input-class+
                                                      spec-input]
                                               :or   {label        "Пароль"
                                                      label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                                      input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                                      spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build input-change-password app {:opts spec-input})
                        )))))

;; END Input password
;;..................................................................................................




;;**************************************************************************************************
;;* BEGIN textarea
;;* tag: <textarea>
;;*
;;* description: Многострочное поле ввода
;;*
;;**************************************************************************************************

(def textarea-app-init
  {:value ""})

(defn textarea-value [app] (get app :value))

(defn textarea [app owner {:keys [class+
                                  onChange-valid?-fn
                                  onKeyPress-fn
                                  placeholder
                                  readonly
                                  required
                                  maxlength
                                  rows
                                  wrap
                                  cols]
                           :or   {class+             ""
                                  onChange-valid?-fn (fn [_ _] true)
                                  onKeyPress-fn      (fn [_] nil)
                                  placeholder        ""
                                  readonly           ""
                                  required           ""
                                  maxlength          1000
                                  rows               "5"
                                  wrap               ""
                                  cols               "40"}}]
  (reify
    om/IRender
    (render [this]
      (dom/textarea #js {:value       (or (:value @app) "")
                         :onChange    (fn [e]
                                        (let [v (.. e -target -value)]
                                          (when (onChange-valid?-fn app v)
                                            (om/update! app :value v))))
                         :onKeyPress  onKeyPress-fn
                         :placeholder placeholder
                         :className   (str "form-control " class+)
                         :readOnly    readonly
                         :required    required
                         :maxLength   maxlength
                         :rows        rows
                         :wrap        wrap
                         :cols        cols
                         }))))

(defn textarea-form-group  [app owner {:keys [label
                                              type
                                              label-class+
                                              input-class+
                                              spec-textarea]
                                       :or   {label         "Метка"
                                              label-class+  "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                              input-class+  "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                              spec-textarea {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build textarea app {:opts spec-textarea})
                        (om/build helper-p app {}))))))


;; END textarea
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN toggle batton
;;* tag: <toggle batton>
;;*
;;* description: кнопка включения и выключения
;;*
;;**************************************************************************************************

(def toggle-button-app-init
  {:value false})

(defn toggle-button [app _ {:keys [bs-type
                                   class+
                                   onClick-fn
                                   text-on
                                   text-off
                                   disabled?]
                            :or   {bs-type :default
                                   class+  ""
                                   text-on "вкл." text-off "выкл."}}]
  (reify
    om/IRender
    (render [_]
      (ui-button {:type      bs-type
                  :active?   (@app :value)
                  :disabled? disabled?
                  :on-click  (fn [_]
                               (om/transact! app :value not)
                               (when onClick-fn (onClick-fn)))
                  :text      (if (@app :value) text-on text-off)}))))


(defn toggle-button-form-group [app owner {:keys [label
                                                  type
                                                  label-class+
                                                  input-class+
                                                  spec-toggle-button]
                                           :or   {label              "Метка"
                                                  label-class+       "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                                  input-class+       "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                                  spec-toggle-button {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build toggle-button app {:opts spec-toggle-button})
                        (om/build helper-p app {}) )))))





(defn toggle-buttons-selector-group-app-init [buttons]
  {:buttons (vec (map
                  (fn [{:keys [value key] :as b} i]
                    (assoc b
                           :value (if (nil? value) false value)
                           :key key))
                  buttons))})

(defn toggle-buttons-selector-get-selected [app]
  (->> app :buttons (filter :value) (map :key)))

(defn toggle-buttons-selector-get-selected-one [app]
  (first (toggle-buttons-selector-get-selected app)))


(defn toggle-buttons-selector-group [app own {:keys [selection-type
                                                     onClick-fn]}]
  (reify
    om/IRender
    (render [_]
      (->> app
           :buttons
           (map (fn [app-row]
                  (let [{:keys [bs-type value disabled? text]} @app-row]
                    (ui-button
                     {:type      (or bs-type :default)
                      :active?   value
                      :disabled? disabled?
                      :on-click
                      (fn [_]
                        (when (= selection-type :one)
                          (om/transact!
                           app :buttons
                           (fn [app] (vec (map #(assoc % :value false) app)))))
                        (om/transact! app-row :value not)
                        (when onClick-fn (onClick-fn)))
                      :text      text}))))
           (apply
            dom/div #js {:className "btn-group" :role "group"})))))


;; END toggle batton
;;..................................................................................................

;;;**************************************************************************************************
;;;* BEGIN many to many input checkers
;;;* tag: <nany to many input checkres>
;;;*
;;;* description: Элемент ввода данных на основании многие ко многим
;;;*
;;;**************************************************************************************************


(def input-select-checkboxes-app-init
  {:data []})


(defn input-select-checkboxes--make-data [app key-text key-value rows]
  (assoc app :data (->> rows
                        (map (fn [row]
                               (assoc row
                                      :value (get row key-value false)
                                      :text  (get row key-text "..."))))
                        vec)))


(defn input-select-checkboxes--get-selected [key-value app]
  (->> app :data (filter key-value)))



(defn input-select-checkboxes [app own
                               {:keys [on-click-fn]
                                :or   {}
                                :as   opts}]
  (reify
    om/IRender
    (render [_]
      (->> app
           :data
           (map (fn [app-row]
                  (letfn [(on-click [_]
                            (om/transact! app-row :value not)
                            (when on-click-fn (on-click-fn)))]
                    (let [{:keys [value text]} @app-row]
                      #_(ui-button
                         {:type      (if value :success :default)
                          :active?   value
                          :disabled? disabled?
                          :block?    true
                          :style     #js {:textAlign "left"}
                          :on-click  (fn [_]
                                       (om/transact! app-row :value not)
                                       (when onClick-fn (onClick-fn)))
                          :text      text})
                      (dom/div
                       #js {:className "checkbox"}
                       (dom/label
                        nil
                        (dom/input #js {:type       "checkbox"
                                        :checked    value
                                        :onClick    (on-click-com-fn on-click)
                                        :onTouchEnd (on-click-com-fn on-click)
                                        })
                        text))))))
           (apply dom/div #js {:className ""})))))


(defn input-select-checkboxes-form-group
  [app own {:keys [label
                   type
                   label-class+
                   input-class+
                   spec-input-select-checkboxes]
            :or   {label                        "Метка"
                   label-class+                 "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                   input-class+                 "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                   spec-input-select-checkboxes {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build input-select-checkboxes app {:opts spec-input-select-checkboxes})
                        (om/build helper-p app {}))))))



;;; END many to many input checkers
;;;..................................................................................................





;;**************************************************************************************************
;;* BEGIN table
;;* tag: <table>
;;*
;;* description: Табличные элементы
;;*
;;**************************************************************************************************



(defn ui-table [{:keys [striped?
                        bordered?
                        condensed?
                        hover?
                        responsive?
                        responsive-class+
                        class+
                        style+
                        thead
                        tbody]
                 :or   {responsive-class+ ""
                        class+            ""}}]
  (let [table (dom/table #js {:className (str "table "
                                              (if striped? "table-striped " "")
                                              (if bordered? "table-bordered " "")
                                              (if condensed? "table-condensed " "")
                                              (if hover? "table-hover " "")
                                              class+)
                              :style     style+}
                         thead
                         tbody)]
    (if responsive?
      (dom/div #js {:className (str "table-responsive " responsive-class+)} table)
      table)))


(defn ui-thead-tr [ths]
  (dom/thead nil (apply dom/tr nil ths)))

(defn ui-tbody [trs]
  (apply dom/tbody nil trs))


(defn tr-sel [app owner {:keys [app-to-tds-seq-fn
                                clear-selections-fn
                                on-select-fn]
                         :or   {app-to-tds-seq-fn
                                (fn [row]
                                  (map
                                   #(dom/td nil %)
                                   (-> row
                                       (select-keys [:id :keyname :description])
                                       vals)))}}]
  (letfn [(on-click [app e]
            (.preventDefault e)
            (.stopPropagation e)

            (when clear-selections-fn
              (clear-selections-fn))

            (omut-row-set-selected-not!! app)

            (when on-select-fn
              (on-select-fn @app))

            1)]

    (reify
      om/IWillMount
      (will-mount [_]
        (omut-row-if-not-init-init!! app))
      om/IRender
      (render [_]
        (apply dom/tr #js {:className (if (omut-row-selected? @app) "info" "")
                           :onClick   (partial on-click app)
                           ;;:onTouchEnd (partial on-click app) ;; недает проматывать
                           }
               (app-to-tds-seq-fn app) )))))




(defn tbody-trs-sel [app owner {:keys [selection-type]
                                :or   {selection-type :one}
                                :as   opts}]
  (reify
    om/IRender
    (render [_]
      (apply
       dom/tbody nil
       (om/build-all
        tr-sel app
        {:opts
         (if (= selection-type :one)
           (assoc opts
                  :clear-selections-fn
                  (fn [_]
                    (om/transact!
                     app
                     (fn [data]
                       ;;(println (map omut-row-key data))
                       (vec (map #(omut-row-set-selected! % false) data))))))
           opts)})))))


;; END table
;;..................................................................................................




;;**************************************************************************************************
;;* BEGIN paginator
;;* tag: <paginator>
;;*
;;* description: Постраничный переключатель
;;*
;;**************************************************************************************************



(def paginator-app-init
  {:page      1
   :page-size 10
   :count-all nil})

(defn paginator [app owner {:keys [chan-update class+ on-click-fn]}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str  "input-group " (if class+ class+ ""))
                    :style     #js {:textAlign "center"
                                    :maxWidth  500
                                    :float     "none"
                                    :margin    "0 auto"}}
               (dom/span #js {:className "input-group-btn"}
                         (ui-button {:type     :default
                                     :on-click (fn [_]
                                                 (om/update! app :page 1)
                                                 (when chan-update
                                                   (put! chan-update 1))

                                                 (when on-click-fn (on-click-fn))
                                                 1)
                                     :text     (dom/span #js {:className   "glyphicon glyphicon-fast-backward"
                                                              :aria-hidden "true"})
                                     })

                         (ui-button {:type     :default
                                     :on-click (fn [_]
                                                 (om/transact! app :page
                                                               #(if (= 1 %) % (dec %)))
                                                 (when chan-update
                                                   (put! chan-update 1))

                                                 (when on-click-fn (on-click-fn))
                                                 1)
                                     :text     (dom/span nil
                                                         (dom/span #js {:className   "glyphicon glyphicon-step-backward"
                                                                        :aria-hidden "true"})
                                                         " Назад")
                                     })
                         )


               (let [{:keys [page page-size count-all]} @app]
                 (dom/div
                  #js {:className "input-control"
                       :style #js {:lineHeight 1.2}}
                  " страница "
                  (dom/b nil page)
                  (when (and count-all page-size) " из ")
                  (when (and count-all page-size) (dom/b nil (inc (quot count-all page-size))))

                  (when count-all (dom/br nil))
                  (when count-all "всего записей ")
                  (when count-all (dom/b nil (str count-all)))))

               (dom/span #js {:className "input-group-btn"}
                         (ui-button {:type     :default
                                     :on-click (fn [_]
                                                 (om/transact! app :page inc)
                                                 (when chan-update
                                                   (put! chan-update 1))

                                                 (when on-click-fn (on-click-fn))
                                                 1)
                                     :text     (dom/span nil "Вперед "
                                                         (dom/span #js {:className   "glyphicon glyphicon-step-forward"
                                                                        :aria-hidden "true"}))
                                     })

                         )))))

;; END paginator
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN Search view
;;* tag: <search view>
;;*
;;* description: Поисковый въювер
;;*
;;**************************************************************************************************


(def search-view-app-init
  (merge {:fts-query input-app-init
          :data      []}
         paginator-app-init))

(defn search-view-app-data [app]
  (@app :data))

(defn search-view-app-data-selected [app]
  (->> @app
       :data
       (filter omut-row-selected?)))

(defn search-view-app-data-selected-first [app]
  (first (search-view-app-data-selected app)))


(defn search-view [app owner
                   {:keys [input-placeholder
                           chan-update
                           data-update-fn
                           data-rendering-fn
                           add-button-fn
                           tools-top
                           tools]
                    :or   {input-placeholder "введите сюда поисковый запрос"
                           data-update-fn    (fn [app]
                                               (println "Неопределена функция запроса обновления данных (data-update-fn [app] ...)")
                                               (println "параметр на входе: " (str app)))
                           data-rendering-fn (fn [app]
                                               (println "Неопределена функция запроса перерисовки данных (data-rendering-fn [app] ...)")
                                               (println "параметр на входе: " (str app)))
                           }}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (or chan-update (chan))})
    om/IWillMount
    (will-mount [this]
      (let [chan-update (om/get-state owner :chan-update)]
        (go (loop []
              (let [_ (<! chan-update)]
                ;; data update function
                (data-update-fn app)

                ;; использовалось при разрабтке
                ;; (ixnet/get-data "/tc/opt/rest/product/search"
                ;;                   {:fts-query (-> @app :fts-query :value)
                ;;                    :page (@app :page)}
                ;;                   (fn [response]
                ;;                     (om/update! app :data response)))

                (recur))))
        (put! chan-update {})))

    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (dom/div
       #js {:className "container-fluid"}
       (dom/div
        #js {:className "row"}

        (when tools-top tools-top)

        (dom/div #js {:className "input-group col-xs-12 col-sm-12 col-md-12 col-lg-12" :style #js {:marginBottom 6}}
                 (dom/span #js {:className "input-group-btn"}
                           (ui-button {:type     :default
                                       :on-click (fn [_]
                                                   (om/update! app :page 1)
                                                   (om/update! app [:fts-query :value] "")
                                                   (put! chan-update 1)
                                                   1)
                                       :text     (dom/span #js {:className   "glyphicon glyphicon-remove"
                                                                :aria-hidden "true"})
                                       }))
                 (om/build input (:fts-query app)
                           {:opts {:placeholder   input-placeholder
                                   :onKeyPress-fn #(do #_(println
                                                          (.-type %)
                                                          (.-which %)
                                                          (.-timeStamp %))

                                                       (when (= 13 (.-which %))
                                                         (do
                                                           (om/update! app :page 1)
                                                           (put! chan-update 1)))
                                                       1)
                                   }})

                 (dom/span #js {:className "input-group-btn"}
                           (ui-button {:type     :success
                                       :on-click (fn [_]
                                                   (om/update! app :page 1)
                                                   (put! chan-update 1)
                                                   1)
                                       :text     (dom/span #js {:className   "glyphicon glyphicon-search"
                                                                :aria-hidden "true"})
                                       })

                           (when add-button-fn
                             (ui-button {:type     :danger
                                         :on-click (fn [_]
                                                     (add-button-fn)
                                                     1)
                                         :text     (dom/span #js {:className   "glyphicon glyphicon-plus"
                                                                  :aria-hidden "true"})
                                         }))

                           )
                 )

        ;;(dom/br nil)
        (when tools tools)

        ;; top paginator
        (dom/div #js {:className "input-group col-xs-12 col-sm-12"}
                 (om/build paginator app {:opts {:chan-update chan-update}}))

        (dom/br nil)

        ;; data rendering component
        (data-rendering-fn app)


        ;; bottom paginator
        (dom/div #js {:className "input-group col-xs-12 col-sm-12"}
                 (om/build
                  paginator app
                  {:opts {:chan-update chan-update
                          ;; При отрабатывании прокручивать страницу на верх
                          :on-click-fn #(.scrollTo js/window 0 0)}}))
        (dom/br nil))))))


;; END Search view
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN Navigation elements
;;* tag: <nav>
;;*
;;* description: Элементы для навигации
;;*
;;**************************************************************************************************

(defn ui-nav [{:keys [brand
                      brand-href]
               :or   {brand      "IX"
                      brand-href "#/"}}
              & body]
  (dom/nav #js {:className "navbar navbar-default navbar-fixed-top"}
           (dom/div #js {:className "container-fluid"}
                    (dom/div #js {:className "navbar-header"}
                             (dom/button #js {:className     "navbar-toggle collapsed"
                                              :type          "button"
                                              :data-toggle   "collapse"
                                              :data-target   "#navbar-collapse-1"
                                              :aria-expanded "false"}
                                         (dom/span #js {:className "sr-only"} "Toggle navigation")
                                         (dom/span #js {:className "icon-bar"})
                                         (dom/span #js {:className "icon-bar"})
                                         (dom/span #js {:className "icon-bar"}))
                             (dom/a #js {:className "navbar-brand" :href brand-href}
                                    brand))


                    (apply dom/div #js {:id        "navbar-collapse-1"
                                        :className "collapse navbar-collapse"}
                           body))))


(defn ui-ul-navbar-nav [& body]
  (apply dom/ul #js {:className "nav navbar-nav"}
         body))

(defn ui-ul-navbar-nav-right [& body]
  (apply dom/ul #js {:className "nav navbar-nav navbar-right"}
         body))


(defn ui-navbar-li [{:keys [glyphicon text href]}]
  (dom/li nil
          (dom/a #js {:href href}
                 (when glyphicon
                   (dom/span #js {:style       #js {:paddingRight 4}
                                  :className   (str "glyphicon " glyphicon)
                                  :aria-hidden "true"}))
                 text)))


(defn ui-navbar-li-dropdown [{:keys [glyphicon text]} & body]
  (dom/li #js {:className "dropdown"}
          (dom/a #js {:href          "#"
                      :className     "dropdown-toggle"
                      :data-toggle   "dropdown"
                      :role          "button"
                      :aria-haspopup "true"
                      :aria-expanded "false"}
                 (when glyphicon
                   (dom/span #js {:style       #js {:paddingRight 4}
                                  :className   (str "glyphicon " glyphicon)
                                  :aria-hidden "true"}))
                 text
                 (dom/span #js {:className "caret"}))
          (apply dom/ul #js {:className "dropdown-menu"} body)))

(defn ui-navbar-li-separator []
  (dom/li #js{:role "separator" :className "divider"}))


(def nav-app-state-key :menu)

(defn nav [app _ opts]
  (letfn [(f1 [{:keys [sub separator?] :as row}]
            (if separator?
              (ui-navbar-li-separator)
              (if (coll? sub)
                (apply (partial ui-navbar-li-dropdown row) (map f1 sub))
                (ui-navbar-li row))))]
    (reify
      om/IRender
      (render [_]
        (let [m (nav-app-state-key app)]
          (ui-nav opts
                  (when-let [menus (:left m)]
                    (apply ui-ul-navbar-nav
                           (map f1 menus)))
                  (when-let [menus (:right m)]
                    (apply ui-ul-navbar-nav-right
                           (map f1 menus)))
                  ))))))


;;------------------------------------------------------------------------------
;; BEGIN: nav tabs page splitter
;; tag: <nav tabs page splitter>
;; description: Табы
;;------------------------------------------------------------------------------

(def nav-tabs-app-state
  {:active-tab 0
   :tabs       [;;{:text "item 1"}
                ]})



(defn nav-tabs-app-state-i-maker [tabs]
  (reduce
   (fn [a [k v]]
     (assoc a k v))
   (vec (range (count tabs)))
   (seq tabs)))

(defn nav-tabs-app-state-init [tabs]
  (assoc nav-tabs-app-state
         :tabs (nav-tabs-app-state-i-maker tabs)))


(defn nav-tabs-active-tab [app]
  (get app :active-tab 0))

(defn nav-tabs-enable-inly-one [app ii]
  (om/transact!
   app (fn [app]
         (-> app
             (update-in [:tabs] #(->> %
                                      (map
                                       (fn [i t]
                                         (if (= ii i) t (assoc t :disabled? true)))
                                       (range))
                                      vec))
             (assoc :active-tab ii)))))

(defn nav-tabs-enable-all [app]
  (om/transact!
   app :tabs
   (fn [tabs] (map #(dissoc % :disabled?) tabs))))


(defn nav-tabs [app _ {:keys [justified?
                              type
                              chan-update]
                       :or   {type "nav-pills"}}]
  (letfn [(on-click [i]
            (on-click-com-fn
             (fn []
               (om/update! app :active-tab i)
               (when chan-update
                 (put! chan-update i)))))]
    (reify
      om/IRender
      (render [_]
        (apply dom/ul #js {:className (str "nav"
                                           (condp = type
                                             :tabs  " nav-tabs"
                                             :pills " nav-pills"
                                             " nav-pills")
                                           (if justified? " nav-justified" ""))}

               (map

                (fn [{:keys [glyphicon text href disabled?]} i]
                  (dom/li #js {:className (if disabled? "disabled"
                                              (if (= i (app :active-tab)) "active" ""))
                               :role      "presentation"}
                          (dom/a #js {:href       href
                                      :onClick    (on-click i)
                                      :onTouchEnd (on-click i)
                                      }
                                 (when glyphicon
                                   (dom/span #js {:style       #js {:paddingRight 4}
                                                  :className   (str "glyphicon " glyphicon)
                                                  :aria-hidden "true"}))
                                 text)))

                (:tabs @app) (range)) )))))



(defn ui-nav-tab [app i body]
  (dom/div #js {:style #js {:display
                            (if (= (:active-tab app) i)
                              "" "none") }
                ;;:data-toggle "dropdown" ;;<- !!! Перестает работать выгрузка файлов, неработает file uploader
                }
           (dom/br nil)
           body))


;; END nav tabs page splitter
;;..............................................................................



;; END Navigation elements
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN Edit form functional
;;* tag: <edit form for id>
;;*
;;* description: Базовые элементы для создания форм и диалогов для редактирования по id
;;*
;;**************************************************************************************************


(def edit-form-for-id-app-init
  {:id nil})

(defn edit-form-for-id [app owner {:keys [chan-load-for-id
                                          uri uri-params+
                                          chan-load-row
                                          chan-save
                                          uri-save
                                          post-save-fn
                                          form-body
                                          fill-app-fn
                                          app-to-row-fn
                                          ]
                                   :or   {url-params+ {}
                                          fill-app-fn
                                          (fn [row]
                                            (println "Функция (fill-app-fn) формирования зароса не определена!"
                                                     " Пришел ответ вида: " row))
                                          app-to-row-fn
                                          (fn []
                                            (println "Функция (app-to-row-fn) формирования зароса не определена!")
                                            {})
                                          }}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-init-row (chan)})
    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-init-row]} (om/get-state owner)]

        (when chan-load-for-id
          (go
            (while true
              (let [id (<! chan-load-for-id)
                    id (if (= id 0) nil id)]
                ;; TODO: сделать отлов ошибок на alert
                (ixnet/get-data
                 uri
                 (merge {:id id} uri-params+)
                 (fn [row]
                   (put! chan-init-row row) ))))))

        (when chan-load-row
          (go
            (while true
              (let [row (<! chan-load-row)]
                (put! chan-init-row row) ))))

        (when chan-init-row
          (go
            (while true
              (let [row (<! chan-init-row)]

                (om/update! app :id (or (:id row) nil))

                (alert-clean app)
                ;; TODO: сделать отлов ошибок на alert
                (fill-app-fn row) ))))

        (when chan-save
          (go
            (while true
              (let [_ (<! chan-save)]
                (println "SAVE!")

                (try
                  (if uri-save
                    ;; если указан то сохранять в сеть
                    (ixnet/get-data
                     uri-save
                     (let [a (app-to-row-fn)]
                       (if-let [id (@app :id)]
                         (assoc a :id id)
                         a))
                     (fn [result]
                       (when post-save-fn (post-save-fn result))
                       ;; SUCCESS MESSAGE
                       (om/transact! app #(assoc % :alert-danger nil :alert-success "Сохранено успешно")))

                     (fn [error-response]
                       (om/transact! app #(assoc % :alert-danger
                                                 (str "Ошибка сохранения: " error-response))))
                     )

                    ;; иначе альтернитиваная функция
                    (when post-save-fn
                      (do (post-save-fn app))
                      ;; SUCCESS MESSAGE
                      (om/transact! app #(assoc % :alert-danger nil :alert-success "Сохранено успешно"))))

                  (catch js/Error e
                    (let [m (str "Ошибка сохранения: " e)]
                      (println m)
                      (om/update! app :alert-danger m))))))))


        ))
    om/IRender
    (render [_]
      (dom/div ;; Common mistakes - Form inside of another form
       #js {:className "form-horizontal col-xs-12 col-sm-12 col-md-12 col-lg-12"}
       ;; HELPER FOR MESSAGES
       (om/build alert app)

       (if form-body
         form-body
         (dom/h1 nil "Элементы формы еще не определены"))))))


(def modal-edit-form-for-id--YN--app-init
  (merge modal-app-init edit-form-for-id-app-init))

(defn modal-edit-form-for-id--YN- [app _ {:keys [new-or-edit-fn?
                                                 edit-form-for-id]
                                          :or   {edit-form-for-id
                                                 (fn [_ _]
                                                   (reify
                                                     om/IRender
                                                     (render [_]
                                                       (dom/h1 nil "Форма диалога еще не указана"))))
                                                 }
                                          :as   opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-save (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-save]}]
      (om/build modal app
                {:opts {:modal-size :lg
                        :label      (if new-or-edit-fn?
                                      (condp = (new-or-edit-fn?)
                                        :new  "Создание новой записи"
                                        :edit "Редактирование записи"
                                        "???")
                                      "Редактирование записи")
                        ;;:modal-size :lg
                        :body
                        (dom/div #js {:className "row"}
                                 (om/build edit-form-for-id app
                                           {:opts (assoc opts
                                                         :chan-save chan-save
                                                         :post-save-fn
                                                         (fn [r]
                                                           (modal-hide app)
                                                           (when-let [post-save-fn-2 (:post-save-fn opts)]
                                                             (post-save-fn-2 r))))}))
                        :footer
                        (dom/div nil
                                 (ui-button {:type     :primary
                                             :on-click (fn [_]
                                                         (put! chan-save 1)
                                                         1)
                                             :text     "Принять"})
                                 (ui-button {:type     :default
                                             :on-click (fn [_] (modal-hide app) 1)
                                             :text     "Отмена"})
                                 )
                        }}))))


(def modal-edit-form-for-id--CLOSE--app-init
  (merge modal-app-init edit-form-for-id-app-init))

(defn modal-edit-form-for-id--CLOSE- [app _ {:keys [new-or-edit-fn?
                                                    edit-form-for-id
                                                    post-save-fn]
                                             :or   {edit-form-for-id
                                                    (fn [_ _]
                                                      (reify
                                                        om/IRender
                                                        (render [_]
                                                          (dom/h1 nil "Форма диалога еще не указана"))))
                                                    }
                                             :as   opts}]
  (reify
    om/IRender
    (render [_]
      (om/build modal app
                {:opts {:modal-size :lg
                        :label      (if new-or-edit-fn?
                                      (condp = (new-or-edit-fn?)
                                        :new  "Создание новой записи"
                                        :edit "Редактирование записи"
                                        "???")
                                      "Редактирование записи")
                        ;;:modal-size :lg
                        :body
                        (dom/div
                         #js {:className "row"}
                         (om/build edit-form-for-id app {:opts opts}))
                        :footer
                        (ui-button {:type     :default
                                    :on-click (fn [_]
                                                (when post-save-fn
                                                  (post-save-fn {}))
                                                (modal-hide app) 1)
                                    :text     "Закрыть"})
                        }}))))



(defn form-show-invalid-messages!! [app message & [input-app input-message]]
  (alert-clean-and-set!! app :alert-danger (or message "Ошибка ввода данных"))
  (when input-app
    (input-css-string-has?-clean-and-set!! input-app :has-error?)
    (helper-p-clean-and-set!!
     input-app :text-danger (or input-message "ошибка ввода"))))




;; END Edit form functional
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN Virtual pages
;;* tag: <virtual pages>
;;*
;;* description: Виртуальные страници для нескольких связанных форм
;;*
;;**************************************************************************************************

(def virtual-pages-app-init
  {:current :main})

(defn virtual-pages-current [app]
  (app :current))

(defn virtual-pages-go-to-page!! [app page]
  (om/update! app :current page))

(defn ui-virtual-page-<<
  ([app page-key body]
   (ui-virtual-page-<< app page-key nil body))
  ([app page-key back-key body]
   (dom/div #js {:style #js {:display
                             (if (= (virtual-pages-current @app) page-key)
                               "" "none") }}
            (when back-key
              (ui-button {:type     :default
                          :on-click (fn [_]
                                      (virtual-pages-go-to-page!! app back-key)
                                      1)
                          :text     (dom/span nil
                                              (dom/span #js {:className   "glyphicon glyphicon-backward"
                                                             :aria-hidden "true"})
                                              " Назад")
                          }))
            body)))


;; END Virtual pages
;;..................................................................................................
;;**************************************************************************************************
;;* BEGIN lists
;;* tag: <lists>
;;*
;;* description: списки для вывода
;;*
;;**************************************************************************************************

(defn ui-list-group [list-groups-items & [class+]]
  (apply dom/ul #js {:className (str "list-group "
                                     (or class+ ""))
                     :style     #js {:marginTop 10}}
         list-groups-items))

(defn ui-list-group-item [{:keys [text type badge active? class+]}]
  (dom/a #js {:className (str  "list-group-item "
                               (get {:success " list-group-item-success"
                                     :info    " list-group-item-info"
                                     :warning " list-group-item-warning"
                                     :danger  " list-group-item-danger"
                                     } type "")
                               (if active? "active" "")
                               " " (or class+ "")
                               )}
         (when badge (dom/span #js {:className "badge"} badge))
         text))



(defn ui-list-group--counts-by [points f
                                {:keys [text nil-text]
                                 :or   {text     "метка в заголовке"
                                        nil-text "не указано"}}]
  (let [items (->> points
                   (group-by f)
                   seq)]
    (ui-list-group
     (reduce
      (fn [a [gi items]]
        (conj a (ui-list-group-item
                 {:text (or gi nil-text) :badge (count items)
                  :type (if (nil? gi) :warning :default)})))
      [(ui-list-group-item {:text text :active? true :badge (count points)})]
      items))))








;; END lists
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN panel
;;* tag: <panel >
;;*
;;* description: панели
;;*
;;**************************************************************************************************

(defn ui-panel [{:keys [heading heading-glyphicon badge
                        body after-body type style class+]}]
  (dom/div #js {:className (str "panel panel-"
                                (get {:default "default"
                                      :primary "primary"
                                      :success "success"
                                      :info    "info"
                                      :warning "warning"
                                      :danger  "danger"
                                      } type "default")
                                (or (str " " class+) ""))
                :style     style}
           (when heading
             (dom/div #js {:className "panel-heading"}
                      (when heading-glyphicon
                        (ui-glyphicon heading-glyphicon))
                      (when heading-glyphicon " ")
                      heading
                      (when badge (dom/span #js {:className "badge"
                                                 :style     #js {:float "right"}} badge))))

           (when body
             (apply
              dom/div #js {:className "panel-body"}
              (if (coll? body) body [body])))

           after-body))

(defn ui-panel-with-table [{:keys [cols rows
                                   striped? bordered? hover? responsive?]
                            :or   {cols        [] rows []
                                   striped?    true
                                   bordered?   true
                                   hover?      true
                                   responsive? true}
                            :as   options}]
  (ui-panel
   (assoc options
          :after-body
          (ui-table
           {:hover?      hover?
            :bordered?   bordered?
            :striped?    striped?
            :responsive? responsive?
            :thead       (->> cols
                              (map #(dom/th nil %))
                              ui-thead-tr)
            :tbody       (->> rows
                              (map (fn [row]
                                     (apply
                                      dom/tr nil
                                      (vec (map #(dom/td nil %) row)))))
                              (apply dom/tbody nil))
            }))))



;; END panel
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Media
;;* tag: <ui media>
;;*
;;* description: Вывод с заголовком и картинкой
;;*
;;**************************************************************************************************

(defn ui-media-object [{:keys [src class+ style]}]
  (dom/img #js {:className (str "media-object " (or class+ ""))
                :style     style
                :src       src
                }))

(defn ui-media [{:keys [media-object
                        heading heading-2
                        on-click-fn
                        on-click-in-image-fn
                        href
                        style
                        body
                        button-do-fn
                        button-do-text
                        heading-tag
                        ]}]
  (dom/div #js {:className "media" :style style
                :onClick   on-click-fn}
           (dom/div #js {:className "media-left"}
                    (dom/a #js {:href    href
                                :onClick (fn [e]
                                           (when on-click-in-image-fn (on-click-in-image-fn))
                                           ;; Далее прервать выполнение события для родительского
                                           ;; компонента
                                           (.stopPropagation e))}
                           media-object))
           (dom/div #js {:className "media-body"}
                    (when button-do-fn (ui-button {:style    #js {:float "right"}
                                                   :type     :primary
                                                   :text     (or button-do-text "Действие")
                                                   :on-click button-do-fn}))
                    (when href (dom/a #js {:style #js {:float "right"}
                                           :href  (or href "#") :target "_blank"}
                                      (dom/button #js {:className "btn btn-success"} "скачать")))
                    (when heading ((or heading-tag dom/h4) #js {:className "media-heading"} heading
                                   (when heading-2 (dom/small nil " - " heading-2 ))))
                    body)))




;; END Media
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN Uploader elements
;;* tag: <uploader>
;;*
;;* description: Элементы для выгрузки файлов
;;*
;;**************************************************************************************************

(defn file-uploder [_ own {:keys [uri
                                  get-uri-fn
                                  update-fn
                                  success-fn
                                  accept]
                           :or   {uri    "/file-uploder/uri"
                                  accept "*.*"}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-upload (chan)
       :form-id     (uniq-id "file-uploder-form")
       :uploader-id (uniq-id "uploder")
       :in-progress false})
    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-upload form-id uploader-id]} (om/get-state own)]
        (go
          (while true
            (let [_ (<! chan-upload)]

              (om/set-state! own :in-progress true)

              (ix-io/file-upload
               (.getElementById js/document form-id)
               (.getElementById js/document uploader-id)
               (if get-uri-fn (get-uri-fn) uri)
               {:success  success-fn
                :complete #(do
                             (when update-fn (update-fn))
                             (om/set-state! own :in-progress false))
                }))))))

    om/IRenderState
    (render-state [_ {:keys [chan-upload form-id uploader-id in-progress]}]
      (dom/form #js {:id      form-id
                     :encType "multipart/form-data"
                     :method  "POST"}
                (dom/span #js {:className "btn btn-default btn-file btn-primary"}
                          "Загрузить"
                          (dom/input #js {:id       uploader-id
                                          :name     "uploader"
                                          :type     "file"
                                          :multiple true
                                          :accept   accept
                                          :onChange #(put! chan-upload 1)
                                          }))
                (when in-progress
                  (dom/span #js {:className "text-warning"}
                            " " (ui-glyphicon "flag") " Подождите, идет выгрузка файлов на сервер..."
                            (dom/img #js {:src "/images/uploading.gif"})))

                ))))

;; END Uploader elements
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN Upload one image
;;* tag: <one image uploader>
;;*
;;* description: Закрузчик одной фотографии
;;*
;;**************************************************************************************************

(def one-image-uploader-app-init {:image nil})

(defn one-image-uploader-value [app]
  (get app :image))

(defn one-image-uploader-value-set! [app v]
  (assoc app :image v))


(defn one-image-uploader [app own {:keys [class+]
                                   :as   opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-upload (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-upload]}]
      (dom/div #js {:className class+}
               (om/build file-uploder app
                         {:opts (merge opts
                                       {:accept     "image/gif, image/jpeg, image/png, image/*"
                                        :update-fn  #(put! chan-upload 1)
                                        :success-fn #(om/update! app :image %)
                                        })})

               (dom/div
                #js {:className "well well-sm"
                     :style     #js {:marginTop 4
                                     :display   "inline-block"}}
                (let [image (@app :image)]
                  (if (empty? image)
                    (ui-glyphicon "camera" "" "8em")
                    (ui-media-object {:class+ "img-rounded"
                                      :style  #js {:maxWidth 300}
                                      :src    (@app :image)}))))
               ))))

(defn one-image-uploader-form-group  [app owner {:keys [label
                                                        label-class+
                                                        input-class+
                                                        spec-one-image-uploader]
                                                 :or   {label                   "Метка"
                                                        label-class+            "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                                        input-class+            "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                                        spec-one-image-uploader {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+}
                        (om/build one-image-uploader app {:opts spec-one-image-uploader})
                        (om/build helper-p app {}) )))))




;; END Upload one image
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Full screen image viewer
;;* tag: <full screen image viewer>
;;*
;;* description: Просмоторщик картинок в полный экран
;;*
;;**************************************************************************************************

(defonce chan-thumb-show-in-full-screen-app-init (chan))

                                        ;TODO: Вынести данную констату в общий файл cljc
(def thumb-show-in-full-screen-id "thumb-show-in-full-screen")



(defonce thumb-show-in-full-screen-app-state
  (atom {:src         nil
         :description nil :top_description nil
         :zoom?       false
         :deg         0}))

(defn- thumb-show-in-full-screen [app]
  (reify
    om/IWillMount
    (will-mount [this]
      (go
        (while true
          (let [{:keys [path src title top_description description]}
                (<! chan-thumb-show-in-full-screen-app-init)]
            (om/transact!
             app #(assoc % :src (or path src)
                         :descrioption description
                         :top_description (or top_description title)))))))
    om/IRender
    (render [_]
      (when-let [src (@app :src)]
        (let [{:keys [deg zoom?]} @app
              deg-2               (str  "rotate(" deg "deg)")]
          (dom/div
           #js {:style #js {:position "fixed" :zIndex           3000
                            :top      0       :bottom           0 :left 0 :right 0
                            :overflow "auto"  :background-color "rgba(0, 0, 0, 0.7)"}}

           (dom/div #js {:style #js {:position "fixed" :left 0 :top 0 :zIndex 3005}}
                    (dom/button #js {:className "close"
                                     :onClick   #(om/transact! app :zoom? not)}
                                (ui-glyphicon (if zoom? "zoom-out" "zoom-in") "" "3em"))
                    (dom/button #js {:className "close"
                                     :onClick   #(om/transact!
                                                  app :deg (fn [deg]
                                                             (let [deg (+ deg 90)]
                                                               (if (> deg 270) 0 deg))))}
                                (ui-glyphicon "retweet" "" "3em")))

           (dom/button #js {:className "close"
                            :style     #js {:position "fixed" :right 0 :top 0 :zIndex 3005}
                            :onClick   (fn []
                                         (om/transact!
                                          app #(assoc % :src nil :descrioption nil
                                                      :top_description nil))
                                         1)}
                       (ui-glyphicon "remove" "" "3em"))

           (dom/img #js {:src   src
                         :style #js {:msTransform     deg-2
                                     :WebkitTransform deg-2
                                     :transform       deg-2
                                     ;;:height (if (#{90 270} deg) (if zoom? nil (.-innerWidth js/window)) nil)
                                     :width           (if zoom? nil (.-innerWidth  js/window))}})))))))


;; Инициализация
(def thumb-show-in-full-screen--tag (by-id thumb-show-in-full-screen-id))

(if thumb-show-in-full-screen--tag
  (do
    (om/root
     thumb-show-in-full-screen
     thumb-show-in-full-screen-app-state
     {:target thumb-show-in-full-screen--tag :opts {}})
    (println "Компонент FULL SCREEN найден и проинициализирован."))
  (println "Компонент FULL SCREEN НЕ найден по id ="
           thumb-show-in-full-screen-id " и не проинициализирован."))




;; END Full screen image viewer
;;..................................................................................................



;;**************************************************************************************************
;;* BEGIN Thumbs
;;* tag: <thumbs>
;;*
;;* description: Функционал работы с тумбами и картинками
;;*
;;**************************************************************************************************


(def thumbnail-app-init
  {:id              nil
   :path            nil
   :top_description nil
   :description     nil
   :galleria        false
   })


(defn thumbnail [app _ {:keys [class+ onClick-fn]
                        :or   {class+ "col-xs-12 col-sm-6 col-md-4 col-lg-4"}}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [id path top_description description galleria] :as row} app
            on-click                                                       (on-click-com-fn #(when onClick-fn (onClick-fn row)))]
        (dom/div
         #js {:className class+}
         (dom/div
          #js {:className  "thumbnail"
               :onClick    on-click
               :onTouchEnd on-click
               :style      #js {:cursor "pointer"}}
          (when galleria
            (dom/span #js {:className   "glyphicon glyphicon-film"
                           :style       #js {:position "absolute"
                                             :top      10 :left 5
                                             :fontSize "2em"}
                           :aria-hidden "true"}))
          (dom/a nil (dom/img #js {:src (str path "_as_300.png") :alt "фото"
                                   ;;:style #js {:width "100%"}
                                   }))
          (dom/div #js {:className "caption"}
                   (when (not (clojstr/blank? top_description))
                     (dom/h4 nil top_description))
                   (dom/div nil
                            (when (not (clojstr/blank? description))
                              (dom/p nil description))

                            (dom/span #js {:className "label label-default"
                                           :style     #js {:width "15%" :fontSize "0.7em"}} "URL")
                            " "
                            (dom/input #js {:type        "text"
                                            :style       #js {:width "65%" :fontSize "0.7em"}
                                            :value       path
                                            :onMouseDown (fn [e] (.select (.-target e)))
                                            }))
                   )))))))

(def thumbnails-edit-form-app-init
  (merge edit-form-for-id-app-init
         {:top_description input-app-init
          :description     textarea-app-init
          :galleria        toggle-button-app-init
          }))


(defn thumbnails-edit-form [app owner opts]
  (reify
    om/IRender
    (render [_]
      (om/build
       edit-form-for-id app
       {:opts
        (merge opts {:fill-app-fn
                     (fn [{:keys [top_description
                                  description
                                  galleria]}]
                       (om/transact!
                        app
                        (fn [app]
                          (-> app
                              ;; Заполнение формы
                              (assoc-in [:top_description :value] top_description)
                              (assoc-in [:description :value] description)
                              (assoc-in [:galleria :value] galleria)))))
                     :app-to-row-fn
                     (fn []
                       {:id              (get @app :id)
                        :top_description (get-in @app [:top_description :value])
                        :description     (get-in @app [:description :value])
                        :galleria        (get-in @app [:galleria :value])})
                     :form-body
                     (dom/fieldset
                      nil
                      (dom/legend nil "Основные данные")

                      (om/build input-form-group (get-in app [:top_description])
                                {:opts {:label "Наименование"}})

                      (om/build toggle-button-form-group (get-in app [:galleria])
                                {:opts {:label "Отображать в галерее"}})

                      (om/build textarea-form-group (get-in app [:description])
                                {:opts {:label "Описание"}})
                      )
                     })
        }))))


(def thumbnails-modal-edit-form-app-init
  (merge modal-edit-form-for-id--YN--app-init thumbnails-edit-form-app-init))

(defn thumbnails-modal-edit-form [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build modal-edit-form-for-id--YN- app
                {:opts (assoc opts :edit-form-for-id thumbnails-edit-form)}))))


(def thumbnails-view-app-init
  {:list         []
   :last-params  {}
   :modal-act    actions-modal-app-init
   :modal-yes-no (assoc modal-yes-no-app-init
                        :row {})
   :modal        thumbnails-modal-edit-form-app-init})

(defn thumbnails-view [app owner {:keys [uri params
                                         uri-upload
                                         uri-delete
                                         chan-update]
                                  :or   {params {}}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-modal-act                              (chan)
       :chan-thumbnails-modal-edit-form-open-for-id (chan)})
    om/IWillMount
    (will-mount [this]
      (when chan-update
        (go
          (while true
            (let [cparams (<! chan-update)
                  p       (if (map? cparams) cparams params)]
              (ixnet/get-data uri p
                              (fn [list]
                                (om/transact!
                                 app #(assoc % :list list :last-params p) ))))))))
    om/IRenderState
    (render-state [_ {:keys [chan-modal-act
                             chan-thumbnails-modal-edit-form-open-for-id]}]
      (dom/div nil
               (om/build file-uploder app
                         {:opts {:accept "image/gif, image/jpeg, image/png, image/*"
                                 :get-uri-fn
                                 #(str
                                   uri-upload
                                   (get-in @app [:last-params :id])
                                   "/image")
                                 :update-fn
                                 #(put! chan-update (:last-params @app))
                                 }})
               (apply
                dom/div #js {:className "row"
                             :style     #js {:margin 5}}
                (map
                 (fn [{:as row}]
                   (om/build thumbnail row
                             {:opts {:onClick-fn
                                     (fn [{:keys [id] :as r}]
                                       (put! chan-modal-act
                                             {:label (str "Выбор действий над записью №" id)
                                              :acts
                                              [{:text "Просмотр" :btn-type :info
                                                :act-fn
                                                (fn []
                                                  (put! chan-thumb-show-in-full-screen-app-init r))
                                                }
                                               {:text "Редактировать" :btn-type :primary
                                                :act-fn
                                                (fn []
                                                  (put! chan-thumbnails-modal-edit-form-open-for-id id)
                                                  (modal-show (:modal app)))}
                                               {:text "Удалить" :btn-type :danger
                                                :act-fn
                                                #(do
                                                   (om/update! app [:modal-yes-no :row] r)
                                                   (modal-show (:modal-yes-no app)))}]
                                              }))}}))
                 (:list app)))

               (om/build actions-modal (:modal-act app) {:opts {:chan-open chan-modal-act}})

               (om/build modal-yes-no (:modal-yes-no app)
                         {:opts {:modal-size :sm
                                 :label      "Желаете удалить изображение?"
                                 :body
                                 (dom/div
                                  #js{:className "row"}
                                  (dom/img #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                                               :src       (get-in @app [:modal-yes-no :row :path])}))
                                 :act-yes-fn
                                 (fn []
                                   (ixnet/get-data
                                    uri-delete ;;"/tc/rb/product/files_rel/delete"
                                    {:id      (get-in @app [:last-params :id])
                                     :file-id (get-in @app [:modal-yes-no :row :id])}
                                    (fn [_]
                                      (when chan-update
                                        (put! chan-update (:last-params @app))))))
                                 }})

               (om/build thumbnails-modal-edit-form (:modal app)
                         {:opts {:chan-load-for-id chan-thumbnails-modal-edit-form-open-for-id
                                 :uri              "/files/find/transit"
                                 :uri-save         "/files/edit/transit"
                                 :post-save-fn     #(do
                                                      (when chan-update
                                                        (put! chan-update (:last-params @app)))
                                                      1)}})

               ))))



(defn images-gallery-1 [app own]
  (reify

    om/IInitState
    (init-state [_]
      {:i          0               :deg            0
       :div-id     (uniq-id "div") :img-id         (uniq-id "img")
       :div-height nil             :img-margin-top nil
       :img-height nil
       })

    om/IDidUpdate
    (did-update [_ _ _]
      )

    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (om/set-state! own :i 0))

    om/IRenderState
    (render-state [_ {:keys [i deg div-id div-height img-id img-margin-top img-height]}]
      (letfn [(img-on-load []
                (println "IMG loaded")
                (om/update-state!
                 own (fn [{:keys [div-id img-id deg] :as state}]
                       (let [r?    (or (= deg 90) (= deg 270))
                             div-w (-> div-id by-id .-clientWidth)
                             div-h (-> div-id by-id .-clientHeight)
                             img-w (-> img-id by-id .-clientWidth)
                             img-h (-> img-id by-id .-clientHeight)
                             fat?  (>= img-w img-h)]
                         (println "REP!")
                         (assoc state                                
                                :img-margin-top (when r?
                                                  (if fat?
                                                    (* 0.5 (- img-w img-h))
                                                    nil))
                                :div-height (when r?
                                              (if fat?
                                                (+ 12 img-w)
                                                nil))
                                :img-height (when (and r? (not fat?))
                                              div-w))))))]
        (let [deg-2 (str  "rotate(" deg "deg)")
              src   (get-in @app [i :path] "")]
          (if (empty? @app)
            (dom/h2 nil "Изображений нет")
            (dom/div
             #js {}
             (dom/b #js {:style #js {:fontSize "26px"}} "Изображение " (inc i) " из " (count @app))


             (dom/div #js {:className "btn-toolbar" :style #js {:float "right"}}
                      (dom/div #js {:className "btn-group"}
                               (ui-button
                                {:text (ui-glyphicon "retweet")
                                 :type :default
                                 :size :lg
                                 :on-click
                                 (fn []
                                   (om/update-state!
                                     own :deg
                                     #(let [deg (+ % 90)]
                                        (if (> deg 270) 0 deg)))
                                   (img-on-load))}))

                      (dom/div #js {:className "btn-group"}
                               (ui-button {:text     (ui-glyphicon "chevron-left")
                                           :type     :default
                                           :size     :lg
                                           :on-click (fn [_]
                                                       (om/update-state!
                                                        own :i
                                                        #(let [i (dec %)]
                                                          (if (< i 0) (dec (count @app)) i)))
                                                       1)})
                               (ui-button {:text     (ui-glyphicon "chevron-right")
                                           :type     :default
                                           :size     :lg
                                           :on-click (fn [_]
                                                       (om/update-state!
                                                        own :i
                                                        #(let [i (inc %)]
                                                           i (if (= i (count @app)) 0 i)))
                                                       1)})))
             (dom/br nil)
             (dom/br nil)
             (dom/div
              #js {:id div-id :className "thumbnail" :style #js {:height div-height}}
              (dom/img #js {:id        img-id
                            :className ""
                            :src       src
                            :style     #js {:msTransform     deg-2
                                            :WebkitTransform deg-2
                                            :transform       deg-2
                                            :marginTop       img-margin-top
                                            :height          img-height}
                            :onClick   #(put! chan-thumb-show-in-full-screen-app-init {:src src})
                            :onLoad    img-on-load}))
             (dom/h2 nil (get-in @app [i :top_description] ""))
             (dom/p nil (get-in @app [i :description] "")))))))))

;; END Thumbs
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN files
;;* tag: <files>
;;*
;;* description: Компоненты работы с файлами
;;*
;;**************************************************************************************************

(def file-thumb-app-init
  {:id              nil
   :path            nil
   :top_description nil
   :description     nil
   :galleria        false
   })


(defn file-thumb [app _ {:keys [class+
                                onClick-fn]
                         :or   {class+ "col-xs-12 col-sm-12 col-md-12 col-lg-12"}}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [id path top_description description galleria] :as row} app
            on-click                                                       (on-click-com-fn #(when onClick-fn (onClick-fn row)))]
        (dom/div
         #js {:className class+}
         (dom/div
          #js {:className  "thumbnail"
               :onClick    on-click
               :onTouchEnd on-click
               :style      #js {:cursor    "pointer"
                                :minHeight 75 }}

          (dom/span #js {:className "glyphicon glyphicon-file"
                         :style     #js {:fontSize   "5em"
                                         :float      "left"
                                         :ariaHidden "true"}})

          (dom/div #js {:className "caption"}
                   (when (not (clojure.string/blank? top_description))
                     (dom/h3 nil top_description))
                   (dom/div nil
                            (when (not (clojure.string/blank? description))
                              (dom/p nil description))

                            (dom/span #js {:className "label label-default"} "URL")
                            " "
                            (dom/input #js {:type        "text"
                                            :style       #js {:width "70%" :fontSize "0.7em"}
                                            :value       path
                                            :onMouseDown (fn [e] (.select (.-target e)))
                                            }))
                   )))))))




(def files-edit-form-app-init
  (merge edit-form-for-id-app-init
         {:top_description input-app-init
          :description     textarea-app-init
          :galleria        toggle-button-app-init
          }))


(defn files-edit-form [app owner opts]
  (reify
    om/IRender
    (render [_]
      (om/build
       edit-form-for-id app
       {:opts
        (merge opts {:fill-app-fn
                     (fn [{:keys [top_description
                                  description
                                  galleria]}]
                       (om/transact!
                        app
                        (fn [app]
                          (-> app
                              ;; Заполнение формы
                              (assoc-in [:top_description :value] top_description)
                              (assoc-in [:description :value] description) ))))
                     :app-to-row-fn
                     (fn []
                       {:id              (get @app :id)
                        :top_description (get-in @app [:top_description :value])
                        :description     (get-in @app [:description :value])})
                     :form-body
                     (dom/fieldset
                      nil
                      (dom/legend nil "Основные данные")

                      (om/build input-form-group (get-in app [:top_description])
                                {:opts {:label "Наименование"}})

                      (om/build textarea-form-group (get-in app [:description])
                                {:opts {:label "Описание"}})
                      )
                     })
        }))))


(def files-modal-edit-form-app-init
  (merge modal-edit-form-for-id--YN--app-init files-edit-form-app-init))

(defn files-modal-edit-form [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build modal-edit-form-for-id--YN- app
                {:opts (assoc opts :edit-form-for-id files-edit-form)}))))


(def files-view-app-init
  {:list         []
   :last-params  {}
   :modal-act    actions-modal-app-init
   :modal-yes-no (assoc modal-yes-no-app-init
                        :row {})
   :modal        files-modal-edit-form-app-init})

(defn files-view [app owner {:keys [uri params
                                    uri-upload
                                    uri-delete
                                    chan-update]
                             :or   {params {}}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-modal-act                         (chan)
       :chan-files-modal-edit-form-open-for-id (chan)})
    om/IWillMount
    (will-mount [this]
      (when chan-update
        (go
          (while true
            (let [cparams (<! chan-update)
                  p       (if (map? cparams) cparams params)]
              (ixnet/get-data uri p
                              (fn [list]
                                (om/transact!
                                 app #(assoc % :list list :last-params p) ))))))))
    om/IRenderState
    (render-state [_ {:keys [chan-modal-act
                             chan-files-modal-edit-form-open-for-id]}]
      (dom/div nil
               (om/build file-uploder app
                         {:opts {:get-uri-fn
                                 #(str
                                   uri-upload
                                   (get-in @app [:last-params :id])
                                   "/file")
                                 :update-fn
                                 #(put! chan-update (:last-params @app))
                                 }})
               (apply
                dom/div #js {:className "row"
                             :style     #js {:margin 5}}
                (map
                 (fn [{:as row}]
                   (om/build file-thumb row
                             {:opts {:onClick-fn
                                     (fn [{:keys [id] :as r}]
                                       (put! chan-modal-act
                                             {:label (str "Выбор действий над записью №" id)
                                              :acts
                                              [{:text   "Редактировать" :btn-type :primary
                                                :act-fn (fn []
                                                          (put! chan-files-modal-edit-form-open-for-id id)
                                                          (modal-show (:modal app)))}
                                               {:text   "Удалить" :btn-type :danger
                                                :act-fn #(do
                                                           (om/update! app [:modal-yes-no :row] r)
                                                           (modal-show (:modal-yes-no app)))}]
                                              }))}}))
                 (:list app)))

               (om/build actions-modal (:modal-act app) {:opts {:chan-open chan-modal-act}})

               (om/build modal-yes-no (:modal-yes-no app)
                         {:opts {:modal-size :sm
                                 :label      "Желаете удалить Фаил?"
                                 :body
                                 (dom/h4 nil (get-in @app [:modal-yes-no :row :filename]))
                                 :act-yes-fn
                                 (fn []
                                   (ixnet/get-data
                                    uri-delete ;;"/tc/rb/product/files_rel/delete"
                                    {:id      (get-in @app [:last-params :id])
                                     :file-id (get-in @app [:modal-yes-no :row :id])}
                                    (fn [_]
                                      (when chan-update
                                        (put! chan-update (:last-params @app))))))
                                 }})

               (om/build files-modal-edit-form (:modal app)
                         {:opts {:chan-load-for-id chan-files-modal-edit-form-open-for-id
                                 :uri              "/files/find/transit"
                                 :uri-save         "/files/edit/transit"
                                 :post-save-fn     #(do
                                                      (when chan-update
                                                        (put! chan-update (:last-params @app)))
                                                      1)}})

               ))))


(def files-table-1-app-init
  [])

(defn files-table-1 [app own]
  (reify
    om/IRender
    (render [_]
      (if (empty? @app)
        (dom/h2 nil "Нет файлов")
        (ui-panel
         {:heading           " Список файлов"
          :badge             (str (count @app))
          :heading-glyphicon "folder-open"
          :type              :primary
          :after-body
          (ui-table
           {:hover?      true
            :bordered?   true
            :striped?    true
            :responsive? true
            :tbody
            (->> @app
                 (map (fn [{:keys [filename path top_description description size]}]
                        (dom/tr nil
                                (dom/td nil
                                        (ui-media {:href         path
                                                   :media-object (ui-glyphicon "file" nil "5em")
                                                   :heading      filename
                                                   :heading-2    top_description
                                                   :body         (dom/p nil description)})))))
                 (apply dom/tbody nil))})})))))

;; END files
;;..................................................................................................


;;;**************************************************************************************************
;;;* BEGIN images and files list viewer with button collapser
;;;* tag: <image-and-files-btn-viewer>
;;;*
;;;* description: Компонент для отображения привязанных файлов и картинок в виде выпадающего через кнопку списка
;;;*
;;;**************************************************************************************************

(defn image-and-files-btn-viewer [app own {:keys [chan-thumb-show-in-full-screen]}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [images files]} @app]
        (ui-collapser
         app (str "изображений (" (count images) ") файлов (" (count files) ")")
         :k (or (not (empty? images)) (not (empty? files)))
         (apply
          dom/ul #js {:className "list-group"}
          (into
           (reduce
            (fn [a {:keys [id path top_description description] :as row}]
              (conj a (dom/li #js {:className "list-group-item"}
                              (ui-media
                               {:media-object         (ui-media-object
                                                       {:src (str path "_as_60.png")})
                                :heading              top_description
                                :on-click-in-image-fn #(put! chan-thumb-show-in-full-screen row)
                                :button-do-text       "cмотреть"
                                :button-do-fn         #(put! chan-thumb-show-in-full-screen row)
                                :body                 description
                                }))))
            [] images)
           (reduce
            (fn [a {:keys [id path filename top_description description] :as row}]
              (conj a (dom/li #js {:className "list-group-item"}
                              (ui-media
                               {:media-object (ui-glyphicon "file" "" "2em")
                                :href         path
                                :on-click-fn
                                (fn [e]
                                  ;; Далее прервать выполнение события для родительского
                                  ;; компонента
                                  (.stopPropagation e))
                                :heading      (dom/span nil top_description
                                                        " " (dom/small nil filename))
                                :body         description
                                }))))
            [] files))))))))

;;; END images and files list viewer with button collapser
;;;..................................................................................................





;;**************************************************************************************************
;;* BEGIN Ввод элементов из справочника
;;* tag: <input rb>
;;*
;;* description: Элементы выбора из справочной таблици
;;*
;;**************************************************************************************************

(defn input-form-search-view-app-init [search-view-app-init]
  {:modal (assoc modal-app-init
                 :search-view search-view-app-init)
   :sel   []
   })

(defn input-form-search-view-get-selected [app]
  (:sel app))

(defn input-form-search-view-clean [app]
  (assoc app :sel []))


(defn input-from-search-view [search-view
                              {:keys [label-one
                                      label-multi
                                      placeholder
                                      class+
                                      on-selected-fn
                                      ui-type
                                      ui-type--add-button--type
                                      ui-type--add-button--text
                                      selection-type
                                      disabled?
                                      multiselect-row-render-fn
                                      row-pk-fiels
                                      one--row-to-text-fn
                                      multi-table-caption
                                      ]
                               :or   {class+                    ""
                                      selection-type            :one
                                      label-one                 "Выбрать ???"
                                      label-multi               "Выбрано ???"
                                      placeholder               "Выберите...."
                                      ui-type                   :input-select
                                      ui-type--add-button--type :default
                                      ui-type--add-button--text "Выбрать..."
                                      row-pk-fiels              [:id]
                                      multi-table-caption       "Наименование"
                                      }}]
  (fn [app _ {:keys [label-one
                     label-multi
                     selection-type
                     ui-type
                     ui-type--add-button--type
                     ui-type--add-button--text
                     on-selected-fn
                     label-class+
                     input-class+
                     search-view-opts
                     main-div-params
                     multi-table-caption]
              :or   {label-one                 label-one
                     label-multi               label-multi
                     selection-type            selection-type
                     ui-type                   ui-type
                     ui-type--add-button--type ui-type--add-button--type
                     ui-type--add-button--text ui-type--add-button--text
                     on-selected-fn            on-selected-fn
                     label-class+              "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                     input-class+              "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                     search-view-opts          {}
                     multi-table-caption       multi-table-caption}}]


    (reify
      om/IRender
      (render [_]
        (dom/div
         main-div-params

         ;;(dom/p nil (str (@app :sel)))

         (condp = ui-type

           :add-button
           (ui-button {:type     ui-type--add-button--type
                       :on-click #(modal-show (:modal app))
                       :text     ui-type--add-button--text })

           :input-select
           (dom/div #js {:className (str "form-group " class+ " "(input-css-string-has? @app))}
                    (dom/label #js {:className (str "control-label " label-class+)}
                               ({:one label-one :multi label-multi} selection-type))

                    (condp = selection-type

                      :one
                      (dom/div
                       #js {:className input-class+
                            :style     #js {}}
                       (dom/div #js {:className "input-group"}
                                (dom/input #js {:value       (let [r (get-in @app [:sel 0])]
                                                               (if one--row-to-text-fn
                                                                 (one--row-to-text-fn r)
                                                                 (:keyname r)))
                                                :placeholder placeholder
                                                :className   "form-control"})
                                (dom/span #js {:className "input-group-btn"}
                                          (ui-button {:type     :default
                                                      :on-click #(modal-show (:modal app))
                                                      :text     (dom/span #js {:className   "glyphicon glyphicon-list-alt"
                                                                               :aria-hidden "true"})})))
                       (om/build helper-p app))

                      :multi
                      (dom/div
                       #js {:className input-class+ :style #js {}}
                       (dom/div
                        #js {:className (str "panel "
                                             ({:muted   "panel-muted"
                                               :primary "panel-primary"
                                               :success "panel-success"
                                               :info    "panel-info"
                                               :warning "panel-warning"
                                               :danger  "panel-danger"}
                                              ui-type--add-button--type))}
                        (dom/div #js {:className "panel-heading"}
                                 (dom/b nil "Выбрано (" (count (@app :sel)) ")")
                                 (om/build helper-p app))
                        (dom/div #js {:className "panel-body" :style #js {:padding 2}}
                                 (ui-table
                                  {:hover?    true
                                   :bordered? true
                                   :striped?  true
                                   ;;:responsive? true
                                   :style+    #js {:marginBottom 0}
                                   :thead     (ui-thead-tr [(dom/th nil multi-table-caption) (dom/th nil "Действие")])
                                   :tbody     (om/build tbody-trs-sel (app :sel)
                                                        {:opts
                                                         {:app-to-tds-seq-fn
                                                          (fn [row]
                                                            (list
                                                             (if multiselect-row-render-fn
                                                               (multiselect-row-render-fn row)
                                                               (dom/td nil (str @row)))
                                                             (dom/td
                                                              #js {:style #js {:width "5%"}}
                                                              (ui-button
                                                               {:text "Удалить"
                                                                :on-click
                                                                (fn [_]
                                                                  (om/transact!
                                                                   app :sel
                                                                   (fn [selected]
                                                                     (let [pk (select-keys @row row-pk-fiels)]
                                                                       (->> selected
                                                                            (filter
                                                                             #(not
                                                                               (= (select-keys % row-pk-fiels)
                                                                                  pk)))
                                                                            vec))))

                                                                  1)}))))
                                                          }})
                                   }))

                        (dom/div #js {:className "panel-footer"}
                                 (ui-button {:type     ui-type--add-button--type
                                             :on-click #(modal-show (:modal app))
                                             :text     ui-type--add-button--text }))))



                      (str "Непонятный selection-type: " selection-type))




                    )

           (str "непонятный тип отображения: " ui-type))


         (om/build modal (:modal app)
                   {:opts {:modal-size :lg
                           :body       (om/build search-view (get-in app [:modal :search-view])
                                                 {:opts (merge {:selection-type selection-type} search-view-opts)})
                           :footer     (dom/div #js {:className "btn-toolbar  pull-right"}
                                                (ui-button
                                                 {:type :primary
                                                  :on-click
                                                  (fn [_]
                                                    (let [selected (->> @app
                                                                        :modal
                                                                        :search-view
                                                                        :data
                                                                        (filter omut-row-selected?))]

                                                      (condp = selection-type
                                                        :multi (om/transact!
                                                                app :sel
                                                                (fn [app]
                                                                  (->> app
                                                                       (map #(omut-row-set-selected! % false))
                                                                       (into selected)
                                                                       (reduce
                                                                        #(assoc %1
                                                                                (select-keys %2 row-pk-fiels)
                                                                                %2)
                                                                        {})
                                                                       vals
                                                                       (sort-by #(-> (select-keys % row-pk-fiels)
                                                                                     vals
                                                                                     sort
                                                                                     vec))
                                                                       reverse
                                                                       vec)))

                                                        :one   (om/update! app :sel (vec selected)))

                                                      (modal-hide (:modal app))

                                                      (when on-selected-fn (on-selected-fn))

                                                      1))
                                                  :text "Выбрать"})

                                                (ui-button {:on-click (fn [_] (modal-hide (:modal app)) 1)
                                                            :text     "Закрыть"})
                                                )
                           }})
         )))))






;; END Ввод элементов из справочника
;;..................................................................................................
