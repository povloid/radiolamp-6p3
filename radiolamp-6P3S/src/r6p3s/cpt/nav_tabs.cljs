(ns r6p3s.cpt.nav-tabs
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]            
            [r6p3s.core :as c]
            [r6p3s.ui.glyphicon :as glyphicon]))



(def app-state
  {:active-tab 0
   :tabs       [;;{:text "item 1"}
                ]})



(defn app-state-i-maker [tabs]
  (reduce
   (fn [a [k v]]
     (assoc a k v))
   (vec (range (count tabs)))
   (seq tabs)))

(defn app-state-init [tabs]
  (assoc app-state
         :tabs (app-state-i-maker tabs)))


(defn active-tab [app]
  (get app :active-tab 0))

(defn set-active-tab [app i]
  (assoc app :active-tab i))

(defn set-active-tab! [app i]
  (om/update! app :active-tab i))

(defn enable-inly-one [app ii]
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

(defn enable-all [app]
  (om/transact!
   app :tabs
   (fn [tabs] (map #(dissoc % :disabled?) tabs))))





(defn component [app _ {:keys [justified?
                               type
                               chan-update]
                        :or   {type "nav-pills"}}]
  (letfn [(on-click [i]
            (c/on-click-com-fn
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

                (fn [{:keys [glyphicon icon text href disabled?]} i]
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
                                 (when icon (glyphicon/render icon))
                                 (when icon " ")                                 
                                 text)))

                (:tabs @app) (range)) )))))
