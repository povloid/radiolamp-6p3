(ns ixinfestor.omut

  (:import [goog.dom query])

  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as aclasses]
            ;;[sablono.core :as html :refer-macros [html]]

            [ixinfestor.net :as ixnet]

            ))



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


(defonce modals-ids (atom 0))
(defn get-modal-id [] (str "modal-" (swap! modals-ids inc)))

(defonce modals-status (atom #{}))
(add-watch
 modals-status :log
 (fn [_ _ old new]
   (when (not= old new)
     (println "modals-status: " new)
     (let[tag-body (aget (query "body") 0)]
       (if (empty? new)
         (goog.dom.classes/remove tag-body "modal-open")
         (goog.dom.classes/add    tag-body "modal-open")))

     (let [d (clojure.set/difference new old)]
       (when (not (empty? d))
         (let [new-dialog-id (first d)]
           (println "Открылся " new-dialog-id)))))))

(defn modal [app owner {:keys [label
                               header
                               body
                               footer
                               class+]
                        :or {label "Пустая пометка"
                             header (dom/h4 #js {:className "modal-title"} "Пустой заголовок")
                             body (dom/p #js {:className "text-info"}
                                         "Пустое пространство диалога. Можно наполнить элементами")
                             footer (dom/button #js {:className "btn btn-default"
                                                     :type "button"
                                                     :onClick (fn [_] (om/update! app :show false) 1)
                                                     :data-dismiss "modal"}
                                                "Закрыть")
                             class+ ""}}]
  (reify
    om/IInitState
    (init-state [_]
      {:id (get-modal-id)})
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
      (println "modal id:" id)
      (let [show? (:show app)]
        (swap! modals-status (if show? conj disj) id)
        (dom/div #js {:id id
                      :aria-hidden "true"
                      :aria-labelledby label
                      :style (if show?
                               #js {:display "block" :paddingLeft 0}
                               #js {:display "none" })
                      :className (if show? "modal in" "modal")
                      :role "dialog"
                      :tabIndex "-1"}
                 (dom/div #js {:className (str "modal-dialog " class+)}
                          (dom/div #js {:className "modal-content"}
                                   (dom/div #js {:className "modal-header"} header)
                                   (dom/div #js {:className "modal-body"}   body)
                                   (dom/div #js {:className "modal-footer"} footer))))))))







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
                          :has-success
                          :has-warning
                          :has-error))))


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
      (dom/input #js {:value (:value app)
                      :onChange (fn [e]
                                  (let [v (.. e -target -value)]
                                    (when (onChange-valid?-fn app v)
                                      (om/update! app :value v))))
                      :onKeyPress onKeyPress-fn
                      :type type
                      :placeholder placeholder
                      :className (str "form-control " class+)}))))



(defn input-form-group  [app owner {:keys [label
                                           class+
                                           type
                                           spec-input]
                                    :or {label "Метка"
                                         class+ ""
                                         spec-input {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " class+)} label)
               (om/build input app spec-input)
               (om/build helper-p app {}) ))))









(defn ui-table [{:keys [striped?
                        bordered?
                        condensed?
                        hover?
                        responsive?
                        responsive-class+
                        class+
                        thead
                        tbody]
                 :or {responsive-class+ ""
                      class+ ""}}]
  (let [table (dom/table #js {:className (str "table "
                                              (if striped? "table-striped " "")
                                              (if bordered? "table-bordered " "")
                                              (if condensed? "table-condensed " "")
                                              (if hover? "table-hover " "")
                                              class+)}
                         thead
                         tbody)]
    (if responsive?
      (dom/div #js {:className (str "table-responsive " responsive-class+)} table)
      table)))


(defn ui-thead-tr [ths]
  (dom/thead nil (apply dom/tr nil ths)))

(defn ui-tbody [trs]
  (apply dom/tbody nil trs))







(def paginator-app-init
  {:page 1})

(defn paginator [app owner {:keys [chan-update]}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "input-group"
                    :style #js {:textAlign "center"}}
               (dom/span #js {:className "input-group-btn"}
                         (dom/button #js {:className "btn btn-default" :type "button"
                                          :onClick (fn [_]
                                                     (om/update! app :page 1)
                                                     (put! chan-update 1)
                                                     1)}
                                     (dom/span #js {:className "glyphicon glyphicon-fast-backward"
                                                    :aria-hidden "true"}))

                         (dom/button #js {:className "btn btn-default" :type "button"
                                          :onClick (fn [_]
                                                     (om/transact! app :page
                                                                   #(if (= 1 %) % (dec %)))
                                                     (put! chan-update 1)
                                                     1)}
                                     (dom/span #js {:className "glyphicon glyphicon-step-backward"
                                                    :aria-hidden "true"})
                                     " Назад")
                         )

               (dom/h4 nil (str "страница " (@app :page)))

               (dom/span #js {:className "input-group-btn"}
                         (dom/button #js {:className "btn btn-default" :type "button"
                                          :onClick (fn [_]
                                                     (om/transact! app :page inc)
                                                     (put! chan-update 1)
                                                     1)}
                                     "Вперед "
                                     (dom/span #js {:className "glyphicon glyphicon-step-forward"
                                                    :aria-hidden "true"}))

                         )))))






(def search-view-app-init
  (merge {:fts-query input-app-init
          :data []}
         paginator-app-init))

(defn search-view [app owner
                   {:keys [data-update-fn
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
      {:chan-update (chan)})

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
                                  (dom/button #js {:className "btn btn-default" :type "button"
                                                   :onClick (fn [_]
                                                              (om/update! app :page 1)
                                                              (om/update! app [:fts-query :value] "")
                                                              (put! chan-update 1)
                                                              1)}
                                              (dom/span #js {:className "glyphicon glyphicon-remove"
                                                             :aria-hidden "true"})))
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
                                  (dom/button #js {:className "btn btn-success" :type "button"
                                                   :onClick (fn [_]
                                                              (om/update! app :page 1)
                                                              (put! chan-update 1)
                                                              1)}
                                              (dom/span #js {:className "glyphicon glyphicon-search"
                                                             :aria-hidden "true"}))

                                  (when add-button-fn
                                    (dom/button #js {:className "btn btn-danger" :type "button"
                                                     :onClick (fn [_]
                                                                (add-button-fn)
                                                                1)}
                                                (dom/span #js {:className "glyphicon glyphicon-plus"
                                                               :aria-hidden "true"})))

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





(defn tr-sel [app owner {:keys [app-to-tds-seq-fn
                                clear-selections-fn]
                         :or {app-to-tds-seq-fn
                              (fn [row]
                                (map
                                 #(dom/td nil %)
                                 (-> row
                                     (select-keys [:id :keyname :description])
                                     vals)))}}]
  (reify
    om/IRender
    (render [_]
      (apply dom/tr #js {:className (if (:tr-selected app) "info" "")
                         :onClick (fn [_]
                                    (when clear-selections-fn
                                      (clear-selections-fn))
                                    (om/transact! app :tr-selected not)
                                    1)}
             (app-to-tds-seq-fn app) ))))




(defn tbody-trs-sel [app owner {:keys [selection-type]
                                :or {selection-type :one}
                                :as opts}]
  (reify
    om/IRender
    (render [_]
      (apply dom/tbody nil
             (om/build-all tr-sel app
                           {:opts (if (= selection-type :one)
                                    (assoc opts
                                           :clear-selections-fn
                                           (fn [_]
                                             (om/transact! app
                                                           (fn [data]
                                                             (vec (map #(assoc % :tr-selected false) data))))))
                                    opts)})))))







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
