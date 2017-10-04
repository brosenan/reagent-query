(ns hiccup-query.core
  (:require [clojure.string :as str]))

(defn elem-and-class-matches [kw elem cls]
  (let [[req-elem & req-cls] (str/split (name kw) #"[.]")
        act-classes (set (str/split cls #" "))]
    (and (or (= req-elem "")
             (=  req-elem (name elem)))
         (every? #(contains? act-classes %) req-cls))))

(defn query-step [vec step]
  (cond (seq? vec)
        (mapcat #(query-step % step) vec)
        :else
        (let [[elem & content] vec
              [attrs content] (cond (map? (first content))
                                    [(first content) (rest content)]
                                    :else
                                    [{} content])
              [exp-elem attr] (str/split (name step) #":")]
          (cond
            (elem-and-class-matches step elem (:class attrs)) content
            (and (not (nil? attr))
                 (= (name elem)  exp-elem)) (list (attrs (keyword attr)))
            :else (list)))))

(defn query [vec & path]
  (loop [res (list vec)
         path path]
    (cond
      (empty? path)
      res
      :else
      (recur (mapcat #(query-step % (first path)) res) (rest path)))))

(defn mock-change-event [val]
  (let [ctrl (js-obj)
        ev (js-obj)]
    (aset ctrl "value" val)
    (aset ev "target" ctrl)
    ev))
