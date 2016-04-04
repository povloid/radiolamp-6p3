(ns ix.omut.ui.navbar-li-separator
  (:require [om.dom :as dom :include-macros true]))


(defn render []
  (dom/li #js{:role "separator" :className "divider"}))
