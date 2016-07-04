(ns r6p3s.ui.nav
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [brand
                      brand-href
                      inverse?]
               :or   {brand      "6П3С"
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
                                        :className (if inverse?
                                                     "collapse navbar-inverse-collapse"
                                                     "collapse navbar-collapse")}
                           body))))
