(ns alert-viewer.core
  (:require [ajax.core :refer [GET POST]]
            [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [cljs-time.format :as tfmt]))

(enable-console-print!)

(defonce app-state (atom {:text "Alert Viewer"
                          :history nil
                          :stats nil
                          :alerts nil}))

(defn ajax-call [url callback]
  (GET url
       {:response-format :json
        :keywords? true
        :handler callback}))



(defn ajax-post [url params callback]
  (POST url
        {:response-format :json
         :format :json
         :keywords? true
         :handler callback
         :params params}))


(defn set-alerts [response]
  (swap! app-state assoc :alerts (get-in response [:hits :hits])))


(defn set-stats [response]
  (swap! app-state assoc :stats response))


(defn current-alerts []
  (ajax-post "http://localhost:9250/.watch_history*/_search"
             {:size 0
              :aggs {:current-alerts {:terms {:field :watch_id}}}}
             #(swap! app-state assoc :alerts
                     (get-in % [:aggregations :current-alerts :buckets]))))


(defn alert-history [id cb]
  (ajax-post "http://localhost:9250/.watch_history*/_search"
             {:filter {:term {:watch_id id}}}
             cb))

(def app-root (. js/document (getElementById "app")) )


(declare build-main)


(defcomponent alert-history-item [{{{:keys [execution_time]} :result} :_source}
                                  _]
  (render [_]
    (dom/tr
     (dom/td (tfmt/unparse
              (tfmt/formatter "MM/dd/YYYY hh:mm:ss a")
              (tfmt/parse (tfmt/formatters :date-time)  execution_time))))))

(defcomponent alert-detail [{:keys [history]} _]
  (render [_]
    (dom/div
     (dom/h2 {:class "subheader"} "Alert Detail")
     (dom/table
      (om/build-all alert-history-item history))
     (dom/a {:href "#"
             :onClick #(build-main)} "Back"))))


(defcomponent alert-list-item [{:keys [key] :as alert} owner]
  (render [_]
    (dom/li
     (dom/a {:href "#"
             :onClick #(alert-history
                        key
                        (fn [r]
                          (swap! app-state assoc :history
                                 (get-in r [:hits :hits]))
                          (om/root alert-detail
                                   {:history (get-in r [:hits :hits])}
                                   {:target app-root})))}
            key))))

(defcomponent stats-summary [{:keys [watcher_state]} _]
  (render [_]
    (dom/div {:class "panel"}
             (dom/h3 {:class "subheader"} "Engine Status")
             (dom/h4 watcher_state))))

(defcomponent alert-list [{:keys [text alerts stats]} owner]
  (render [_]
    (dom/div
     (dom/h2 "Alert Viewer")
     (dom/div {:class "large-6 columns"}
       (dom/div {:class "panel"}
                (dom/h3 {:class "subheader"} "Active Alerts")
                (dom/ul
                 (om/build-all alert-list-item alerts))))
     (dom/div {:class "large-6 columns"}
              (om/build stats-summary stats)))))


(defn build-main []
  (om/root alert-list app-state {:target app-root}))

(defn main []
  (current-alerts)
  (build-main))
