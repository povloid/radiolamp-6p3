(ns r6p3s.cpt.select-time-interval
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-input :as common-input]
            
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.ui.form-group :as form-group]
            
            [r6p3s.cpt.modal :as modal]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.helper-p :as helper-p]
            [r6p3s.cpt.input-datetime :as input-datetime]
            [r6p3s.cpt.modal-yes-no :as modal-yes-no]))



(defn- set-input-datetime-value [app k d]
  (assoc-in app [k :value] (c/format-date input-datetime/date-str-format d)))

(defn- get-input-datetime-value [app k]
  (-> app k input/value input-datetime/parse-str-to-date))


(defn app-init-fn [from-time to-time]
  {:interval {:from-time from-time
              :to-time   to-time}
   :modal    (-> modal-yes-no/app-init
                 (set-input-datetime-value :from-time from-time)
                 (set-input-datetime-value :to-time   to-time))})


(defn get-selected [app]
  (:interval app))





(defn interval-as-string [from-time to-time]
  (let [[d h m] (c/the-time-has-passed-from from-time to-time)]
    (str d " сут. " h " час. " m " мин. ")))





(defn component [app own {:keys [placeholder selected-fn] :as opts}]
  (reify
    om/IRender
    (render [_]
      (let [app-v                       @app
            {:keys [from-time to-time]} (get-selected app-v)
            app-modal                   (app :modal)]
        (dom/div #js {:className "well well-sm"
                      :style     #js {:backgroundColor "inherit"}}

                 (button/render {:type     :default
                                 :style    #js {:float "right"}
                                 :on-click (fn []                                             
                                             (om/transact!
                                              app-modal
                                              (fn [app-modal]
                                                (-> app-modal
                                                    (set-input-datetime-value :from-time from-time)
                                                    (set-input-datetime-value :to-time   to-time))))
                                             (modal/show app-modal))
                                 :text     (glyphicon/render "time")})

                 (dom/div #js {:className "text-primary"}
                          (dom/div #js{:style #js {:width 20 :float "left"}} "от")
                          (c/format-date input-datetime/date-str-format from-time)
                          (dom/br nil)
                          (dom/div #js{:style #js {:width 20 :float "left"}} "до")
                          (c/format-date input-datetime/date-str-format to-time))
                 (dom/b nil "выбрано: ")
                 (interval-as-string from-time to-time)
                 
                 

                 (let [from-time (get-input-datetime-value @app-modal :from-time)
                       to-time   (get-input-datetime-value @app-modal :to-time)]
                   (om/build modal-yes-no/component app-modal
                             {:opts {:label "Указать интервал времени"
                                     
                                     :body
                                     (dom/div
                                      #js{:className "row"}
                                      (om/build input-datetime/component-form-group (app-modal :from-time)
                                                {:opts {:label      "От"
                                                        :spec-input {:type               "datetime-local" :placeholder "yyyy-MM-ddTHH:mm"
                                                                     :onChange-valid?-fn common-input/vldfn-not-empty-date}}})
                                      (om/build input-datetime/component-form-group (app-modal :to-time)
                                                {:opts {:label      "До "
                                                        :spec-input {:type               "datetime-local" :placeholder "yyyy-MM-ddTHH:mm"
                                                                     :onChange-valid?-fn common-input/vldfn-not-empty-date}}})

                                      (form-group/render
                                       {:label "Установлена продолжитльность"
                                        :body  (dom/b #js {:className "text-primary"}                                                     
                                                      (interval-as-string from-time to-time))}))

                                     :act-yes-fn
                                     (fn []
                                       (om/update!
                                        app :interval
                                        {:from-time from-time 
                                         :to-time   to-time})
                                       (modal/hide app-modal)
                                       (when selected-fn (selected-fn)))
                                     }})))))))




(defn component-form-group  [app _ {:keys [label
                                           type
                                           label-class+
                                           input-class+
                                           spec-input]
                                    :or   {label        "Метка"
                                           label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                           input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                           spec-select  {}}}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-input})
                        (om/build helper-p/component app {}))))))
