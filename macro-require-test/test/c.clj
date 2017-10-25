(ns test.c
    (:require [clojure.string :as string]))

(defn join-special [things]
      (->> things
           (mapv str)
           (string/join "~~~~")))