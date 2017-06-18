(ns r6p3s.ui.markdown-text
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.dom :as dom :include-macros true]
            [markdown.core :refer [md->html]]))





(defn render [text]
  (dom/div #js {:style                   #js {:overflow "auto" :padding 5}
                :dangerouslySetInnerHTML #js {:__html (md->html text)}}
           nil))
