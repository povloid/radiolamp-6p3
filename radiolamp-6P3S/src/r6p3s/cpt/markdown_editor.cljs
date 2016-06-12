(ns r6p3s.cpt.markdown-editor
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.helper-p :as helper-p]
            [r6p3s.ui.nav-tab :as nav-tab]
            [r6p3s.cpt.nav-tabs :as nav-tabs]
            [markdown.core :refer [md->html]]))


(def app-init
  {:value ""
   :tabs  (nav-tabs/app-state-init
           {0 {:text "Рaзметка"
               :icon "pencil"}
            1 {:text "Вид"
               :icon "eye-open"}})})

(defn app-init-value [value]
  {:value value
   :tabs  (nav-tabs/app-state-init
           {0 {:text "Рaзметка"
               :icon "pencil"}
            1 {:text "Вид"
               :icon "eye-open"}})})



(defn component [app own
                 {:keys [class+ placeholder
                         maxlength
                         rows
                         wrap
                         cols]
                  :or   {class+ ""
                         placeholder ""
                         maxlength   1000000
                         rows        6
                         wrap        ""
                         cols        40} :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (chan)})

    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-update]} (om/get-state own)]
        (println "OM: component -> will-mount")

        (go
          (while true
            (let [_ (<! chan-update)]
              (println "OM: component -> chan-update -> run! "))))

        (put! chan-update 1)))

    om/IDidMount
    (did-mount [_]
      (let [{:keys []} (om/get-state own)]
        (println "OM: component -> did-mount")))

    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys []} (om/get-state own)]
        (println "OM: component -> will-unmount")))


    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (dom/div
       #js {:className ""
            :style #js {:minHeight 200
                        :overflow "auto"
                        :borderStyle "ridge"
                        :borderRadius 5
                        :padding 2}}

       (om/build nav-tabs/component (:tabs app)
                 {:opts {:type :tabs :justified? false}})

       (nav-tab/render
        (@app :tabs) 0
        (dom/textarea
         #js {:value    (or (:value @app) "")
              :onChange (fn [e]
                          (let [v (.. e -target -value)]
                            (om/update! app :value v)))
                                        ;:onKeyPress  onKeyPress-fn

              :placeholder placeholder
              :className   "form-control"

              :maxLength maxlength
              :wrap      wrap
              :rows      rows
              :cols      cols}))

       (nav-tab/render
        (@app :tabs) 1
        (dom/div #js {:style #js {:overflow "auto" :padding 5}
                      :dangerouslySetInnerHTML
                      #js {:__html (str (when (-> @app :tabs nav-tabs/active-tab (= 1))
                                          (md->html (@app :value))))}}
                 nil))))))





(defn component-form-group [app owner {:keys [label
                                              type
                                              label-class+
                                              input-class+
                                              spec-mark-dorwn]
                                       :or   {label         "Метка"
                                              label-class+  "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                              input-class+  "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                              spec-mark-dorwn {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-mark-dorwn})
                        (om/build helper-p/component app {}))))))
