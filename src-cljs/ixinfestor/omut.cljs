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


(defn parse-int-or-nil [v]
  (if (is-numeric? v)
    (js/parseInt v)
    nil))



(defn on-click-com-fn [f]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (f)
    1))


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
  (om/transact! app omut-row-key
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
  (om/update! app-row [omut-row-key :collapsed k] v))

(defn omut-row-set-collapsed-not!! [app-row k]
  (om/transact! app-row [omut-row-key :collapsed]
                (fn [app-row]
                  (if app-row (update-in app-row [k] not) {k false}))))

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



;; END Dates and Times
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN icons
;;* tag: <icons>
;;*
;;* description: Иконки
;;*
;;**************************************************************************************************

(defn ui-glyphicon [name & [class+ size]]
  (dom/span #js {:className (str "glyphicon glyphicon-" name " " (or class+ ""))
                 :style #js {:fontSize size}
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
                         on-click]
                  :or {text "Кнопка"
                       type :default}}]
  (dom/button #js {:className (str "btn "
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
                   :type "button"
                   :disabled (if disabled? "disabled" "")
                   :onClick    (on-click-com-fn on-click)
                   :onTouchEnd (on-click-com-fn on-click)
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
                               class+]
                        :or {label "Пустая пометка"
                             modal-size :default
                             class+ "" }
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
        (dom/div #js {:id id
                      :aria-hidden "true"
                      :aria-labelledby label
                      :style (if show?
                               #js {:display "block"
                                    :paddingLeft 0
                                    :backgroundColor "rgba(0, 0, 0, 0.2)"}
                               #js {:display "none" })
                      :className (if show? "modal in" "modal")
                      :role "dialog"
                      :tabIndex "-1"}
                 (dom/div #js {:className (str "modal-dialog"
                                               (condp = modal-size
                                                 :sm " modal-sm"
                                                 :lg " modal-lg"
                                                 "")
                                               " " class+)}
                          (dom/div #js {:className "modal-content"}
                                   (dom/div #js {:className "modal-header"}
                                            (or header (dom/h4 #js {:className "modal-title"} label)))
                                   (dom/div #js {:className "modal-body"
                                                 :style #js {:marginRight 40}}
                                            (or body (dom/p #js {:className "text-info"}
                                                            "Пустое пространство диалога. Можно наполнить элементами")))
                                   (dom/div #js {:className "modal-footer"}
                                            (or footer (ui-button {:type :default
                                                                   :on-click (fn [_] (om/update! app :show false) 1)
                                                                   :text "Закрыть"}))))))))))


;;------------------------------------------------------------------------------
;; BEGIN: buttons modal for select actions variants
;; tag: <buttons modal for select actions variants>
;; description: Диалог для выбора вариантов действия
;;------------------------------------------------------------------------------

(defn actions-modal-button [app _ {:keys [text
                                          btn-type
                                          act-fn]
                                   :or {text "Метка события"
                                        btn-type :default}}]
  (reify
    om/IRender
    (render [_]
      (ui-button {:text text
                  :type btn-type
                  :block? true
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
        :acts []}})
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
                {:opts {:label label
                        :modal-size :sm
                        :body (let [buttons (map
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
                               :or {act-yes-fn #(js/alert "Действие еще не реализовано")}
                               :as opts}]
  (reify
    om/IRender
    (render [_]
      (om/build modal app
                {:opts (assoc opts :footer
                              (dom/div nil

                                       (ui-button {:type :primary
                                                   :on-click (fn [_]
                                                               (act-yes-fn)
                                                               (modal-hide app) 1)
                                                   :text "Да"})

                                       (ui-button {:on-click (fn [_] (modal-hide app) 1)
                                                   :text  "Нет"})

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
                          :body (dom/p #js {:className type-class+}
                                       " " (@app :message))}})))))


(defn show-in-message-modal [type {:keys [title message] :as message-row}]
  (let [_ (or (by-id message-modal-id)
              (let [error-div (.createElement js/document "div")
                    tag-body (aget (query "body") 0)]
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


(defn text-collapser [app owner {k :k}]
  (reify
    om/IWillMount
    (will-mount [_]
      (omut-row-if-not-init-init!! app))
    om/IRender
    (render [_]
      (let [text (app k "")
            text-count (count text)]
        (dom/p nil
               (ui-button {:text "..."
                           :size :xs
                           :active? (not (omut-row-collapsed? @app k))
                           :on-click #(omut-row-set-collapsed-not!! app k)})

               (if (and (omut-row-collapsed? @app k) (> text-count 90))
                 (str (.substring text 0 89) "...")
                 text))))))

;; END Long text collapser
;;..................................................................................................




;;**************************************************************************************************
;;* BEGIN helper
;;* tag: <helper>
;;*
;;* description: Текстовые подсказки
;;*
;;**************************************************************************************************

(def helper-p-app-init
  {:text-muted nil
   :text-primary nil
   :text-success nil
   :text-info nil
   :text-warning nil
   :text-danger nil})


(defn helper-p [app _]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [text-muted
                    text-primary
                    text-success
                    text-info
                    text-warning
                    text-danger]} app]
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
  {:alert-muted nil
   :alert-primary nil
   :alert-success nil
   :alert-info nil
   :alert-warning nil
   :alert-danger nil})


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
   :has-error? nil})


(defn input-css-string-has? [{:keys [has-success?
                                     has-warning?
                                     has-error?]}]
  (condp = true
    has-error?  "has-error"
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

;; END has
;;..................................................................................................

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

(defn input [app owner {:keys [class+
                               type
                               onChange-valid?-fn
                               onChange-updated-fn
                               onKeyPress-fn
                               placeholder]
                        :or {class+ ""
                             type "text"
                             onChange-valid?-fn (fn [_ _] true)
                             onKeyPress-fn (fn [_] nil)
                             placeholder ""
                             }}]
  (reify
    om/IRender
    (render [this]
      (let [value (:value @app)]
        (dom/input #js {:value value
                        :onChange (fn [e]
                                    (let [new-value (.. e -target -value)]
                                      (if (onChange-valid?-fn app new-value)
                                        (om/update! app :value new-value)
                                        (om/update! app :value value))
                                      (when onChange-updated-fn
                                        (onChange-updated-fn))
                                      ))
                        :onKeyPress onKeyPress-fn
                        :type type
                        :placeholder placeholder
                        :className (str "form-control " class+)})))))

(defn input-form-group  [app owner {:keys [label
                                           type
                                           label-class+
                                           input-class+
                                           spec-input]
                                    :or {label "Метка"
                                         label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                         input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                         spec-input {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className label-class+ } label)
               (dom/div #js {:className input-class+ :style #js {:padding 0}}
                        (om/build input app {:opts spec-input})
                        (om/build helper-p app {})
                        )))))




(defn input-vldfn-not-empty [app v]
  (helper-p-clean app)
  (input-css-string-has?-clean app)
  (when (= (count (.trim v)) 0)
    (om/transact! app #(assoc % :has-warning? true :text-warning "Пустое поле")))
  true)

;; END input
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN input select
;;* tag: <input select>
;;*
;;* description: Выбор из списка
;;*
;;**************************************************************************************************

(def select-app-init
  {:selected nil
   :list []})

(def no-select-v "NO-SELECT")

(defn select-app-list-set! [app new-list]
  (assoc app :list new-list))

(defn select-app-selected-set! [app selected]
  (assoc app :selected (str selected)))

(defn select-app-selected [app]
  (let [sv (app :selected)]
    (if (or (nil? sv)
            (empty? sv)
            (= sv no-select-v)) nil sv)))


(defn select [app _ {:keys [on-change-fn]}]
  (reify
    om/IRender
    (render [_]
      ;;(println "SELECT APP:" @app)
      (dom/select
       #js {:value (@app :selected)
            :className "form-control"
            :onChange
            (fn [e]
              (let [v (-> e .-target .-value)]
                (om/update! app :selected v)
                (when on-change-fn (on-change-fn v))))}

       (doall
        (map (fn [{:keys [id title keyname]}]
               (println id title keyname)
               (dom/option #js {:value (str id)} keyname " " title))
             (into [{:id no-select-v :title "Выбрать..."}] (@app :list)))) ))))

(defn select-form-group  [app _ {:keys [label
                                        type
                                        label-class+
                                        input-class+
                                        spec-select]
                                 :or {label "Метка"
                                      label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                      input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                      spec-select {}}}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className label-class+ } label)
               (dom/div #js {:className input-class+ :style #js {:padding 0}}
                        (om/build select app {:opts spec-select})
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

         (om/build input (:password-1 app) {:opts {:type "password"
                                                   :onChange-updated-fn onChange-updated-fn}})
         (om/build input (:password-2 app) {:opts {:type "password"
                                                   :onChange-updated-fn onChange-updated-fn}})
         (om/build helper-p app {})

         )))))

(defn input-change-password-group  [app owner {:keys [label
                                                      type
                                                      label-class+
                                                      input-class+
                                                      spec-input]
                                               :or {label "Пароль"
                                                    label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                                    input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                                    spec-input {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className label-class+ } label)
               (dom/div #js {:className input-class+ :style #js {:padding 0}}
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
                           :or {class+ ""
                                onChange-valid?-fn (fn [_ _] true)
                                onKeyPress-fn (fn [_] nil)
                                placeholder ""
                                readonly ""
                                required ""
                                maxlength 1000
                                rows "5"
                                wrap ""
                                cols "40"}}]
  (reify
    om/IRender
    (render [this]
      (dom/textarea #js {:value (:value app)
                         :onChange (fn [e]
                                     (let [v (.. e -target -value)]
                                       (when (onChange-valid?-fn app v)
                                         (om/update! app :value v))))
                         :onKeyPress onKeyPress-fn
                         :placeholder placeholder
                         :className (str "form-control " class+)
                         :readOnly readonly
                         :required required
                         :maxLength maxlength
                         :rows rows
                         :wrap wrap
                         :cols cols
                         }))))

(defn textarea-form-group  [app owner {:keys [label
                                              type
                                              label-class+
                                              input-class+
                                              spec-textarea]
                                       :or {label "Метка"
                                            label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                            input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                            spec-textarea {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className label-class+ } label)
               (dom/div #js {:className input-class+ :style #js {:padding 0}}
                        (om/build textarea app {:opts spec-textarea})
                        (om/build helper-p app {}) )))))


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
                                   disabled?]
                            :or {bs-type :default
                                 class+ ""}}]
  (reify
    om/IRender
    (render [_]
      (ui-button {:type bs-type
                  :active? (app :value)
                  :disabled? disabled?
                  :on-click (fn [_]
                              (om/transact! app :value not)
                              (when onClick-fn (onClick-fn)))
                  :text (if (app :value) "Вкл." "Выкл.")}
                 ))))


(defn toggle-button-form-group [app owner {:keys [label
                                                  type
                                                  label-class+
                                                  input-class+
                                                  spec-toggle-button]
                                           :or {label "Метка"
                                                label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                                input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                                spec-toggle-button {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className label-class+ } label)
               (dom/div #js {:className input-class+ :style #js {:padding 0}}
                        (om/build toggle-button app {:opts spec-toggle-button})
                        (om/build helper-p app {}) )))))

;; END toggle batton
;;..................................................................................................


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
                 :or {responsive-class+ ""
                      class+ ""}}]
  (let [table (dom/table #js {:className (str "table "
                                              (if striped? "table-striped " "")
                                              (if bordered? "table-bordered " "")
                                              (if condensed? "table-condensed " "")
                                              (if hover? "table-hover " "")
                                              class+)
                              :style style+}
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
                         :or {app-to-tds-seq-fn
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
        (apply dom/tr #js {:className  (if (omut-row-selected? @app) "info" "")
                           :onClick    (partial on-click app)
                           :onTouchEnd (partial on-click app)}
               (app-to-tds-seq-fn app) )))))




(defn tbody-trs-sel [app owner {:keys [selection-type]
                                :or {selection-type :one}
                                :as opts}]
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
                       (println (map omut-row-key data))
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
  {:page 1})

(defn paginator [app owner {:keys [chan-update]}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "input-group"
                    :style #js {:textAlign "center"}}
               (dom/span #js {:className "input-group-btn"}
                         (ui-button {:type :default
                                     :on-click (fn [_]
                                                 (om/update! app :page 1)
                                                 (when chan-update
                                                   (put! chan-update 1))
                                                 1)
                                     :text (dom/span #js {:className "glyphicon glyphicon-fast-backward"
                                                          :aria-hidden "true"})
                                     })

                         (ui-button {:type :default
                                     :on-click (fn [_]
                                                 (om/transact! app :page
                                                               #(if (= 1 %) % (dec %)))
                                                 (when chan-update
                                                   (put! chan-update 1))
                                                 1)
                                     :text (dom/span nil
                                                     (dom/span #js {:className "glyphicon glyphicon-step-backward"
                                                                    :aria-hidden "true"})
                                                     " Назад")
                                     })
                         )

               (dom/h4 nil (str "страница " (@app :page)))

               (dom/span #js {:className "input-group-btn"}
                         (ui-button {:type :default
                                     :on-click (fn [_]
                                                 (om/transact! app :page inc)
                                                 (when chan-update
                                                   (put! chan-update 1))
                                                 1)
                                     :text (dom/span nil "Вперед "
                                                     (dom/span #js {:className "glyphicon glyphicon-step-forward"
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
          :data []}
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
                   {:keys [chan-update
                           data-update-fn
                           data-rendering-fn
                           add-button-fn]
                    :or {data-update-fn (fn [app]
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
      (dom/div nil
               (dom/div #js {:className "input-group"}
                        (dom/span #js {:className "input-group-btn"}
                                  (ui-button {:type :default
                                              :on-click (fn [_]
                                                          (om/update! app :page 1)
                                                          (om/update! app [:fts-query :value] "")
                                                          (put! chan-update 1)
                                                          1)
                                              :text (dom/span #js {:className "glyphicon glyphicon-remove"
                                                                   :aria-hidden "true"})
                                              }))
                        (om/build input (:fts-query app)
                                  {:opts {:onKeyPress-fn #(do #_(println
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
                                  (ui-button {:type :success
                                              :on-click (fn [_]
                                                          (om/update! app :page 1)
                                                          (put! chan-update 1)
                                                          1)
                                              :text (dom/span #js {:className "glyphicon glyphicon-search"
                                                                   :aria-hidden "true"})
                                              })

                                  (when add-button-fn
                                    (ui-button {:type :danger
                                                :on-click (fn [_]
                                                            (add-button-fn)
                                                            1)
                                                :text (dom/span #js {:className "glyphicon glyphicon-plus"
                                                                     :aria-hidden "true"})
                                                }))

                                  )

                        )
               (dom/br nil)
               (om/build paginator app {:opts {:chan-update chan-update}})
               (dom/br nil)

               ;; data rendering component
               (data-rendering-fn app)
               ;; Использовалось при разработке
               ;; #_(ui-table {:hover? true
               ;;              :bordered? true
               ;;              :striped? true
               ;;              :responsive? true
               ;;              :thead (ui-thead-tr [(dom/th nil "Наименование")
               ;;                                   (dom/th nil "Описание")])
               ;;              :tbody (apply dom/tbody nil
               ;;                            (for [r (:data app)]
               ;;                              (dom/tr nil
               ;;                                      (dom/td nil (r :id))
               ;;                                      (dom/td nil (r :keyname)))))
               ;;              })



               ))))


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
               :or {brand "IX"
                    brand-href "#/"}}
              & body]
  (dom/nav #js {:className "navbar navbar-default navbar-fixed-top"}
           (dom/div #js {:className "container-fluid"}
                    (dom/div #js {:className "navbar-header"}
                             (dom/button #js {:className "navbar-toggle collapsed"
                                              :type "button"
                                              :data-toggle "collapse"
                                              :data-target "#navbar-collapse-1"
                                              :aria-expanded "false"}
                                         (dom/span #js {:className "sr-only"} "Toggle navigation")
                                         (dom/span #js {:className "icon-bar"})
                                         (dom/span #js {:className "icon-bar"})
                                         (dom/span #js {:className "icon-bar"}))
                             (dom/a #js {:className "navbar-brand" :href brand-href}
                                    brand))


                    (apply dom/div #js {:id "navbar-collapse-1"
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
                   (dom/span #js {:style #js {:paddingRight 4}
                                  :className (str "glyphicon " glyphicon)
                                  :aria-hidden "true"}))
                 text)))


(defn ui-navbar-li-dropdown [{:keys [glyphicon text]} & body]
  (dom/li #js {:className "dropdown"}
          (dom/a #js {:href "#"
                      :className "dropdown-toggle"
                      :data-toggle "dropdown"
                      :role "button"
                      :aria-haspopup "true"
                      :aria-expanded "false"}
                 (when glyphicon
                   (dom/span #js {:style #js {:paddingRight 4}
                                  :className (str "glyphicon " glyphicon)
                                  :aria-hidden "true"}))
                 text
                 (dom/span #js {:className "caret"}))
          (apply dom/ul #js {:className "dropdown-menu"} body)))

(defn ui-navbar-li-separator []
  (dom/li #js{:role "separator" :className "divider"}))


(def nav-app-state-key :menu)

(defn nav [app _]
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
          (ui-nav {}
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
   :tabs [;; {:text "item 1"}
          ]})


(defn nav-tabs-app-state-i-maker [tabs]
  (reduce
   (fn [a [k v]]
     (assoc a k v))
   (vec (range (count tabs)))
   (seq tabs)))

(defn nav-tabs-active-tab [app]
  (get-in @app [:active-tab] 0))

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
                       :or {type "nav-pills"}}]
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
                               :role "presentation"}
                          (dom/a #js {:href href
                                      :onClick    (on-click i)
                                      :onTouchEnd (on-click i)
                                      }
                                 (when glyphicon
                                   (dom/span #js {:style #js {:paddingRight 4}
                                                  :className (str "glyphicon " glyphicon)
                                                  :aria-hidden "true"}))
                                 text)))

                (:tabs app) (range)) )))))


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
                                          uri
                                          chan-load-row
                                          chan-save
                                          uri-save
                                          post-save-fn
                                          form-body
                                          fill-app-fn
                                          app-to-row-fn
                                          ]
                                   :or {fill-app-fn
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
                 {:id id}
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
      (dom/div
       #js {:className "row"}
       (dom/div
        #js {:className "col-sm-12 col-md-12 col-lg-12"}

        ;; HELPER FOR MESSAGES
        (om/build alert app)

        (dom/form
         #js {:className "form-horizontal col-sm-12 col-md-12 col-lg-12"}
         (if form-body
           form-body
           (dom/h1 nil "Элементы формы еще не определены"))))))))


(def modal-edit-form-for-id--YN--app-init
  (merge modal-app-init edit-form-for-id-app-init))

(defn modal-edit-form-for-id--YN- [app _ {:keys [new-or-edit-fn?
                                                 edit-form-for-id]
                                          :or {edit-form-for-id
                                               (fn [_ _]
                                                 (reify
                                                   om/IRender
                                                   (render [_]
                                                     (dom/h1 nil "Форма диалога еще не указана"))))
                                               }
                                          :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-save (chan)})
    om/IRenderState
    (render-state [_ {:keys[chan-save]}]
      (om/build modal app
                {:opts {:label (if new-or-edit-fn?
                                 (condp = (new-or-edit-fn?)
                                   :new "Создание новой записи"
                                   :edit "Редактирование записи"
                                   "???")
                                 "Редактирование записи")
                        ;;:modal-size :lg
                        :body
                        (om/build edit-form-for-id app
                                  {:opts (assoc opts
                                                :chan-save chan-save
                                                :post-save-fn
                                                (fn [r]
                                                  (modal-hide app)
                                                  (when-let [post-save-fn-2 (:post-save-fn opts)]
                                                    (post-save-fn-2 r))))})
                        :footer
                        (dom/div nil
                                 (ui-button {:type :primary
                                             :on-click (fn [_]
                                                         (put! chan-save 1)
                                                         1)
                                             :text "Принять"})
                                 (ui-button {:type :default
                                             :on-click (fn [_] (modal-hide app) 1)
                                             :text "Отмена"})
                                 )
                        }}))))


(def modal-edit-form-for-id--CLOSE--app-init
  (merge modal-app-init edit-form-for-id-app-init))

(defn modal-edit-form-for-id--CLOSE- [app _ {:keys [new-or-edit-fn?
                                                    edit-form-for-id
                                                    post-save-fn]
                                             :or {edit-form-for-id
                                                  (fn [_ _]
                                                    (reify
                                                      om/IRender
                                                      (render [_]
                                                        (dom/h1 nil "Форма диалога еще не указана"))))
                                                  }
                                             :as opts}]
  (reify
    om/IRender
    (render [_]
      (om/build modal app
                {:opts {:label (if new-or-edit-fn?
                                 (condp = (new-or-edit-fn?)
                                   :new "Создание новой записи"
                                   :edit "Редактирование записи"
                                   "???")
                                 "Редактирование записи")
                        ;;:modal-size :lg
                        :body
                        (om/build edit-form-for-id app {:opts opts})
                        :footer
                        (ui-button {:type :default
                                    :on-click (fn [_]
                                                (when post-save-fn
                                                  (post-save-fn {}))
                                                (modal-hide app) 1)
                                    :text "Закрыть"})
                        }}))))



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
  {:current :main
   })

(defn virtual-pages-current [app]
  (@app :current))

(defn virtual-pages-go-to-page [app page]
  (om/update! app :current page))

(defn ui-virtual-page-<<
  ([app page-key body]
   (ui-virtual-page-<< app page-key nil body))
  ([app page-key back-key body]
   (dom/div #js {:style #js {:display
                             (if (= (virtual-pages-current app) page-key)
                               "" "none") }}
            (when back-key
              (ui-button {:type :default
                          :on-click (fn [_]
                                      (virtual-pages-go-to-page app back-key)
                                      1)
                          :text (dom/span nil
                                          (dom/span #js {:className "glyphicon glyphicon-backward"
                                                         :aria-hidden "true"})
                                          " Назад")
                          }))
            body)))


;; END Virtual pages
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Uploader elements
;;* tag: <uploader>
;;*
;;* description: Элементы для выгрузки файлов
;;*
;;**************************************************************************************************

(defn file-uploder [_ owner {:keys [uri
                                    get-uri-fn
                                    update-fn
                                    accept]
                             :or {uri "/file-uploder/uri"
                                  accept "*.*"}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-upload (chan)
       :form-id (uniq-id "file-uploder-form")
       :uploader-id (uniq-id "uploder")})
    om/IWillMount
    (will-mount [this]
      (let [{:keys[chan-upload form-id]} (om/get-state owner)]
        (go
          (while true
            (let [_ (<! chan-upload)]
              (ix-io/file-upload
               (.getElementById js/document form-id)
               (if get-uri-fn (get-uri-fn)
                   uri)
               {:success
                #(do
                   (when update-fn (update-fn))
                   (println "UPLOAD SUCCES!!!"))})
              )))))

    om/IRenderState
    (render-state [_ {:keys [chan-upload form-id uploader-id]}]
      (dom/form #js {:id form-id
                     :encType "multipart/form-data"
                     :method "POST"}
                (dom/span #js {:className "btn btn-default btn-file btn-primary"
                               }
                          "Загрузить"
                          (dom/input #js {:id uploader-id
                                          :name "uploader"
                                          :type "file"
                                          :multiple true
                                          :accept accept
                                          :onChange #(put! chan-upload 1)
                                          }))))))

;; END Uploader elements
;;..................................................................................................




;;**************************************************************************************************
;;* BEGIN Thumbs
;;* tag: <thumbs>
;;*
;;* description: Функционал работы с тумбами и картинками
;;*
;;**************************************************************************************************


(def thumbnail-app-init
  {:id nil
   :path nil
   :top_description nil
   :description nil
   :galleria false
   })


(defn thumbnail [app _ {:keys [class+ onClick-fn]
                        :or {class+ "col-xs-6 col-sm-4 col-md-4 col-lg-4"}}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [id path top_description description galleria] :as row} app
            on-click (on-click-com-fn #(when onClick-fn (onClick-fn row)))]
        (dom/div
         #js {:className class+}
         (dom/div
          #js {:className "thumbnail"
               :onClick    on-click
               :onTouchEnd on-click
               :style #js {:cursor "pointer"}}
          (when galleria
            (dom/span #js {:className "glyphicon glyphicon-film"
                           :style #js {:position "absolute"
                                       :top 10 :left 5
                                       :fontSize "2em"}
                           :aria-hidden "true"}))
          (dom/a nil (dom/img #js {:src path :alt "фото"}))
          (dom/div #js {:className "caption"}
                   (when (not (clojstr/blank? top_description))
                     (dom/h3 nil top_description))
                   (dom/div nil
                            (when (not (clojstr/blank? description))
                              (dom/p nil description))

                            (dom/span #js {:className "label label-default"} "URL")
                            " "
                            (dom/input #js {:type "text"
                                            :style #js {:width "70%" :fontSize "0.7em"}
                                            :value path
                                            :onMouseDown (fn [e] (.select (.-target e)))
                                            }))
                   )))))))


(def thumbnails-edit-form-app-init
  (merge edit-form-for-id-app-init
         {:top_description input-app-init
          :description textarea-app-init
          :galleria toggle-button-app-init
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
                                {:opts{:label "Отображать в галерее"}})

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
  {:list []
   :last-params {}
   :modal-act actions-modal-app-init
   :modal-yes-no (assoc modal-yes-no-app-init
                        :row {})
   :modal thumbnails-modal-edit-form-app-init})

(defn thumbnails-view [app owner {:keys [uri params
                                         uri-upload
                                         uri-delete
                                         chan-update]
                                  :or {params {}}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-modal-act (chan)
       :chan-thumbnails-modal-edit-form-open-for-id (chan)})
    om/IWillMount
    (will-mount [this]
      (when chan-update
        (go
          (while true
            (let [cparams (<! chan-update)
                  p (if (map? cparams) cparams params)]
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
                             :style #js {:margin 5}}
                (map
                 (fn [{:as row}]
                   (om/build thumbnail row
                             {:opts {:onClick-fn
                                     (fn [{:keys [id] :as r}]
                                       (put! chan-modal-act
                                             {:label (str "Выбор действий над записью №" id)
                                              :acts
                                              [{:text "Редактировать" :btn-type :primary
                                                :act-fn (fn []
                                                          (put! chan-thumbnails-modal-edit-form-open-for-id id)
                                                          (modal-show (:modal app)))}
                                               {:text "Удалить" :btn-type :danger
                                                :act-fn #(do
                                                           (om/update! app [:modal-yes-no :row] r)
                                                           (modal-show (:modal-yes-no app)))}]
                                              }))}}))
                 (:list app)))

               (om/build actions-modal (:modal-act app) {:opts {:chan-open chan-modal-act}})

               (om/build modal-yes-no (:modal-yes-no app)
                         {:opts {:modal-size :sm
                                 :label "Желаете удалить изображение?"
                                 :body
                                 (dom/div
                                  #js{:className "row"}
                                  (dom/img #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                                               :src (get-in @app [:modal-yes-no :row :path])}))
                                 :act-yes-fn
                                 (fn []
                                   (ixnet/get-data
                                    uri-delete ;;"/tc/rb/product/files_rel/delete"
                                    {:id (get-in @app [:last-params :id])
                                     :file-id (get-in @app [:modal-yes-no :row :id])}
                                    (fn [_]
                                      (when chan-update
                                        (put! chan-update (:last-params @app))))))
                                 }})

               (om/build thumbnails-modal-edit-form (:modal app)
                         {:opts {:chan-load-for-id chan-thumbnails-modal-edit-form-open-for-id
                                 :uri      "/files/find/transit"
                                 :uri-save "/files/edit/transit"
                                 :post-save-fn #(do
                                                  (when chan-update
                                                    (put! chan-update (:last-params @app)))
                                                  1)}})

               ))))

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
  {:id nil
   :path nil
   :top_description nil
   :description nil
   :galleria false
   })


(defn file-thumb [app _ {:keys [class+
                                onClick-fn]
                         :or {class+ "col-xs-12 col-sm-6 col-md-6 col-lg-6"}}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [id path top_description description galleria] :as row} app
            on-click (on-click-com-fn #(when onClick-fn (onClick-fn row)))]
        (dom/div
         #js {:className class+}
         (dom/div
          #js {:className "thumbnail"
               :onClick    on-click
               :onTouchEnd on-click
               :style #js {:cursor "pointer"
                           :minHeight 75 }}

          (dom/span #js {:className "glyphicon glyphicon-file"
                         :style #js {:fontSize "5em"
                                     :float "left"
                                     :ariaHidden "true"}})

          (dom/div #js {:className "caption"}
                   (when (not (clojure.string/blank? top_description))
                     (dom/h3 nil top_description))
                   (dom/div nil
                            (when (not (clojure.string/blank? description))
                              (dom/p nil description))

                            (dom/span #js {:className "label label-default"} "URL")
                            " "
                            (dom/input #js {:type "text"
                                            :style #js {:width "70%" :fontSize "0.7em"}
                                            :value path
                                            :onMouseDown (fn [e] (.select (.-target e)))
                                            }))
                   )))))))




(def files-edit-form-app-init
  (merge edit-form-for-id-app-init
         {:top_description input-app-init
          :description textarea-app-init
          :galleria toggle-button-app-init
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
  {:list []
   :last-params {}
   :modal-act actions-modal-app-init
   :modal-yes-no (assoc modal-yes-no-app-init
                        :row {})
   :modal files-modal-edit-form-app-init})

(defn files-view [app owner {:keys [uri params
                                    uri-upload
                                    uri-delete
                                    chan-update]
                             :or {params {}}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-modal-act (chan)
       :chan-files-modal-edit-form-open-for-id (chan)})
    om/IWillMount
    (will-mount [this]
      (when chan-update
        (go
          (while true
            (let [cparams (<! chan-update)
                  p (if (map? cparams) cparams params)]
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
                             :style #js {:margin 5}}
                (map
                 (fn [{:as row}]
                   (om/build file-thumb row
                             {:opts {:onClick-fn
                                     (fn [{:keys [id] :as r}]
                                       (put! chan-modal-act
                                             {:label (str "Выбор действий над записью №" id)
                                              :acts
                                              [{:text "Редактировать" :btn-type :primary
                                                :act-fn (fn []
                                                          (put! chan-files-modal-edit-form-open-for-id id)
                                                          (modal-show (:modal app)))}
                                               {:text "Удалить" :btn-type :danger
                                                :act-fn #(do
                                                           (om/update! app [:modal-yes-no :row] r)
                                                           (modal-show (:modal-yes-no app)))}]
                                              }))}}))
                 (:list app)))

               (om/build actions-modal (:modal-act app) {:opts {:chan-open chan-modal-act}})

               (om/build modal-yes-no (:modal-yes-no app)
                         {:opts {:modal-size :sm
                                 :label "Желаете удалить Фаил?"
                                 :body
                                 (dom/h4 nil (get-in @app [:modal-yes-no :row :filename]))
                                 :act-yes-fn
                                 (fn []
                                   (ixnet/get-data
                                    uri-delete ;;"/tc/rb/product/files_rel/delete"
                                    {:id (get-in @app [:last-params :id])
                                     :file-id (get-in @app [:modal-yes-no :row :id])}
                                    (fn [_]
                                      (when chan-update
                                        (put! chan-update (:last-params @app))))))
                                 }})

               (om/build files-modal-edit-form (:modal app)
                         {:opts {:chan-load-for-id chan-files-modal-edit-form-open-for-id
                                 :uri      "/files/find/transit"
                                 :uri-save "/files/edit/transit"
                                 :post-save-fn #(do
                                                  (when chan-update
                                                    (put! chan-update (:last-params @app)))
                                                  1)}})

               ))))

;; END files
;;..................................................................................................




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
   :sel []
   })

(defn input-form-search-view-get-selected [app]
  (:sel app))


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
                                      ]
                               :or {class+ ""
                                    selection-type :one
                                    label-one   "Выбрать ???"
                                    label-multi "Выбрано ???"
                                    placeholder "Выберите...."
                                    ui-type :input-select
                                    ui-type--add-button--type :default
                                    ui-type--add-button--text "Выбрать..."
                                    row-pk-fiels [:id]
                                    }}]
  (fn [app _ {:keys [selection-type
                     ui-type
                     ui-type--add-button--type
                     ui-type--add-button--text
                     on-selected-fn]
              :or {selection-type selection-type
                   ui-type ui-type
                   ui-type--add-button--type ui-type--add-button--type
                   ui-type--add-button--text ui-type--add-button--text
                   on-selected-fn on-selected-fn
                   }}]


    (reify
      om/IRender
      (render [_]
        (dom/div
         nil

         ;;(dom/p nil (str (@app :sel)))

         (condp = ui-type

           :add-button
           (ui-button {:type ui-type--add-button--type
                       :on-click #(modal-show (:modal app))
                       :text ui-type--add-button--text })

           :input-select
           (dom/div #js {:className (str "form-group " class+ " "(input-css-string-has? app))}
                    (dom/label #js {:className "control-label col-sm-3 col-md-2 col-lg-1"} ({:one label-one :multi label-multi} selection-type))

                    (condp = selection-type

                      :one (dom/div #js {:className "input-group col-sm-9 col-md-10 col-lg-11"}
                                    (dom/input #js {:value (get-in @app [:sel 0 :keyname])
                                                    :placeholder placeholder
                                                    :className "form-control"})
                                    (dom/span #js {:className "input-group-btn"}
                                              (ui-button {:type :default
                                                          :on-click #(modal-show (:modal app))
                                                          :text (dom/span #js {:className "glyphicon glyphicon-list-alt"
                                                                               :aria-hidden "true"})})))

                      :multi
                      (dom/div
                       #js {:className "col-sm-9 col-md-10 col-lg-11" :style #js {:padding 0}}
                       (dom/div
                        #js {:className (str "panel "
                                             ({:muted   "panel-muted"
                                               :primary "panel-primary"
                                               :success "panel-success"
                                               :info    "panel-info"
                                               :warning "panel-warning"
                                               :danger  "panel-danger"}
                                              ui-type--add-button--type))}
                        (dom/div #js {:className "panel-heading"} (dom/b nil "Выбрано (" (count (@app :sel)) ")"))
                        (dom/div #js {:className "panel-body" :style #js {:padding 2}}
                                 (ui-table
                                  {:hover? true
                                   :bordered? true
                                   :striped? true
                                   ;;:responsive? true
                                   :style+ #js {:marginBottom 0}
                                   :thead (ui-thead-tr [(dom/th nil "Наименование") (dom/th nil "Действие")])
                                   :tbody (om/build tbody-trs-sel (app :sel)
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
                                 (ui-button {:type ui-type--add-button--type
                                             :on-click #(modal-show (:modal app))
                                             :text ui-type--add-button--text }))))



                      (str "Непонятный selection-type: " selection-type))




                    )

           (str "непонятный тип отображения: " ui-type))

         (om/build helper-p app)


         (om/build modal (:modal app)
                   {:opts {:body (om/build search-view (get-in app [:modal :search-view])
                                           {:opts {:selection-type selection-type}})
                           :footer (dom/div #js {:className "btn-toolbar  pull-right"}
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
                                                        :text "Закрыть"})
                                            )
                           }})
         )))))






;; END Ввод элементов из справочника
;;..................................................................................................
