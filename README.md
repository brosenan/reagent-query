# reagent-query

A helper library for testing Reagent code as pure functions 

## Installation

Add the following to your leiningen / boot project:

[![Clojars Project](https://img.shields.io/clojars/v/brosenan/reagent-query.svg)](https://clojars.org/brosenan/reagent-query)

## Overview

[Reagent](http://reagent-project.github.io) is a ClojureScript library that wraps [React](https://reactjs.org), and makes it even more awesome by leveraging the power of
Clojure's functional programming.

Reagent allows the client-side of a web application to consist of nothing but pure ClojureScript functions that map the _state_, stored in special atoms,
to a [hiccup](https://github.com/weavejester/hiccup)-like representation of HTML.
To make the application interactive, event handlers such as `on-click` or `on-change` can be assigned ClojureScript functions (closures), 
which will be executed once the event fires.

Previous to ClojureScript, my last client-side experience involved [AngularJS](https://angularjs.org) (1.X).
Although React (and Reagent) present an overall better programming model than Angular (IMHO), one thing is missing in React/Reagent, 
and that's a coherent approach for unit-testing an application.

This was rather surprising and disapointing.
After all, the code is reduced to pure ClojureScript functions, so why can't I just test them as functions?

The problem is that the data structure they emit -- the hiccup-like vectors -- are not so easy to test.
Of course I can compare an entire vector to an expected one, but this will make me duplicate the code in the test, which is something I'd rather avoid.
Furthermore, event handlers are functions, and their comparison is identity-based, meaning that even if I create the exact same event handler in the test,
it will still test different than the one created by the function I'm testing.

## Documentation
Documentation can be found [here](https://brosenan.github.io/reagent-query/core.html).

## Examples
> This example is taken from the [documentation](https://brosenan.github.io/reagent-query/core.html).
> Please refer to the [original examle](https://brosenan.github.io/reagent-query/core.html#example) for up-to-date usage.

Consider the following function defining a component in Reagent:
```Clojure
(defn todo [state]
  [:ul
   (for [{:keys [id todo]} @state]
     [:li {:key id}
      [:input {:value todo
               :on-change #(swap! state
                                  (partial map
                                           (fn [x]
                                             (if (= (:id x) id)
                                               (assoc x :todo (.-target.value %))
                                               x))))}]
      [:button {:on-click #(swap! state
                                  (partial filter
                                           (fn [x]
                                             (not= (:id x) id))))} "Done"]])])
```


The following test uses `reagent-query` to test the above function, without using reagent.

```Clojure
(deftest todo-example
      ;; Empty list
      (let [state (atom [])]
        (is (= (-> (todo state)
                   (rq/query :ul :li))
               [])))

      ;; The :id field should be the :li's :key attribute
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/query :ul :li:key))
               [1 2])))

      ;; Each element includes an :input box with the :todo value as :value
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/query :ul :li :input:value))
               ["One" "Two"])))

      ;; Each :li element has a :button with "Done" as text
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/query :ul :li :button))
               ["Done" "Done"])))

      ;; The :on-click callback associated with each button deletes the respective entry in the atom
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])
            callbacks (-> (todo state)
                          (rq/query :ul :li :button:on-click))]
        ;; Let's call the second callback
        ((second callbacks))
        ;; Now we should only have "One"
        (is (= (-> (todo state)
                   (rq/query :ul :li :input:value))
               ["One"])))

      ;; The :on-change callback of the :input box update the :todo of that entry
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])
            callbacks (-> (todo state)
                          (rq/query :ul :li :input:on-change))]
        ;; Let's call the first callback with a mock event modifying the value to "Three"
        ((first callbacks) (rq/mock-change-event "Three"))
        ;; Now we should have "Three" instead of "One"
        (is (= (-> (todo state)
                   (rq/query :ul :li :input:value))
               ["Three" "Two"]))))
```

## License
Copyright © 2017 Boaz Rosenan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
