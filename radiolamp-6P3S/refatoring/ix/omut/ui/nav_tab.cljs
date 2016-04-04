(ns ix.omut.ui.nav-tab
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


(defn div [app i body]
  (dom/div #js {:style #js {:display
                            (if (= (:active-tab app) i)
                              "" "none") }
                ;;:data-toggle "dropdown" ;;<- !!! Перестает работать выгрузка файлов, неработает file uploader
                }
           (dom/br nil)
           body))
