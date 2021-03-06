(ns reagent-query.core-test
  (:require [cljs.test :refer-macros [is testing]]
            [devcards.core :refer-macros [deftest]]
            [reagent-query.core :as rq])
  (:require-macros [reagent-query.tests :refer [fact]]))

[[:chapter {:title "Introduction"}]]
"[Reagent](http://reagent-project.github.io) is a ClojureScript library that wraps [React](https://reactjs.org), and makes it even more awesome by leveraging the power of
Clojure's functional programming."

"Reagent allows the client-side of a web application to consist of nothing but pure ClojureScript functions that map the _state_, stored in special atoms,
to a [hiccup](https://github.com/weavejester/hiccup)-like representation of HTML.
To make the application interactive, event handlers such as `on-click` or `on-change` can be assigned ClojureScript functions (closures), 
which will be executed once the event fires."

[[:section {:title "The Problem"}]]
"Previous to ClojureScript, my last client-side experience involved [AngularJS](https://angularjs.org) (1.X).
Although React (and Reagent) present an overall better programming model than Angular (IMHO), one thing is missing in React/Reagent, 
and that's a coherent approach for unit-testing an application."

"This was rather surprising and disapointing.
After all, the code is reduced to pure ClojureScript functions, so why can't I just test them as functions?"

"The problem is that the data structure they emit -- the hiccup-like vectors -- are not so easy to test.
Of course I can compare an entire vector to an expected one, but this will make me duplicate the code in the test, which is something I'd rather avoid.
Furthermore, event handlers are functions, and their comparison is identity-based, meaning that even if I create the exact same event handler in the test,
it will still test different than the one created by the function I'm testing."

[[:section {:title "The Solution"}]]
"This library is intended to provide functions to extract information from hiccup-like vectors.
By being able to concisely query the returned vectors to extract the expected information we can write concise tests that exercise the component functions
outside the full context of the application.
After all, these are just functions.
We can set values to atoms, call a function extract data and assert its value.
We can extract event handlers and call them to see how they change the state, etc."

[[:section {:title "Example"}]]
"In the following example we will show a piece of Reagent code and the associated tests, based on `reagent-query`."

"The following component function takes as parameter an atom containing its state.
The state consists of a sequence of \"todo\" tasks, given as maps, each containing a unique `:id` and a `:todo` string.
The component creates a `:div` containing a `:ul` with a `:li` for each task, with an `:input` box containing the text and a `:button` for deleting the task.
In addition, the `:div` also contains a `:button` for creating a new task."

(defn todo [state]
  [:div
   [:ul
    (for [{:keys [id todo]} @state]
      [:li {:key id
            :class "task"}
       [:input {:value todo
                :on-change #(swap! state
                                   (partial map
                                            (fn [x]
                                              (if (= (:id x) id)
                                                (assoc x :todo (.-target.value %))
                                                x))))}]
       [:button {:class "delete-task"
                 :on-click #(swap! state
                                   (partial filter
                                            (fn [x]
                                              (not= (:id x) id))))} "Done"]])]
   [:button {:class "add-task"
             :on-click #(swap! state
                               conj {:id (->> @state
                                              (map :id)
                                              (reduce max)
                                              inc)
                                     :todo ""})}
    "Add Task"]])

"These are the tests:"
(fact todo-example
      ;; Empty list
      (let [state (atom [])]
        (is (= (-> (todo state)
                   (rq/find :li.task))
               [])))

      ;; The :id field should be the :li's :key attribute
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/find :li:key))
               [1 2])))

      ;; Each element includes an :input box with the :todo value as :value
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/find :input:value))
               ["One" "Two"])))

      ;; Each :li element has a :button with "Done" as text
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/find :button.delete-task))
               ["Done" "Done"])))

      ;; The :on-click callback associated with each button deletes the respective entry in the atom
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])
            callbacks (-> (todo state)
                          (rq/find :button.delete-task:on-click))]
        ;; Let's call the second callback
        ((second callbacks))
        ;; Now we should only have "One"
        (is (= (-> (todo state)
                   (rq/find :li.task :input:value))
               ["One"])))

      ;; The :on-change callback of the :input box update the :todo of that entry
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])
            callbacks (-> (todo state)
                          (rq/find :li.task :input:on-change))]
        ;; Let's call the first callback with a mock event modifying the value to "Three"
        ((first callbacks) (rq/mock-change-event "Three"))
        ;; Now we should have "Three" instead of "One"
        (is (= (-> (todo state)
                   (rq/find :li.task :input:value))
               ["Three" "Two"])))

      ;; An "Add Task" button adds a new (empty) task
      (let [state (atom [{:id 1 :todo "One"}
                         {:id 2 :todo "Two"}])]
        (is (= (-> (todo state)
                   (rq/find :button.add-task)) ["Add Task"]))
        ;; We click it
        (let [[add] (-> (todo state)
                        (rq/find :button.add-task:on-click))]
          (add)
          ;; Now we should have a third element
          (is (= (-> (todo state)
                     (rq/find :li.task:key))
                 [1 2 3]))
          ;; With an empty 
          (is (= (-> (todo state)
                     (rq/find :li.task :input:value))
                 ["One" "Two" ""])))))

[[:chapter {:title "query"}]]
"`query` is given a hiccup-like vector and zero or more additional arguments, and returns a sequence of results."

"The idea comes from [jQuery](https://jquery.com), where the `$` function (among other things) acts as a query function,
returning a sequence of results based on the given path.
Our `query` function is intended to do the same, based on a given hiccup-like vector."

"When given no extra arguments, `query` returns a single result: the given vector."
(fact q1
      (is (= (rq/query [:div "foo" "bar"]) [[:div "foo" "bar"]])))

"When given an extra keyword argument matching the element type (the first element in the vector), the child elements are returned."
(fact q2
      (is (= (rq/query [:div "foo" "bar"] :div) ["foo" "bar"])))

"If the extra parameter does not match the element type, an empty sequence is returned."
(fact q3
      (is (= (rq/query [:div "foo" "bar"] :p) [])))

"According to hiccup's convention, if the vector's second element is a map, it represents _attributes_ for the element.
If the extra argument is a keyword of the form `:elem:attr`, and if the `:elem` part matches the element,
the value of the attribute `:attr` is returned."
(fact q4
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :div:id) ["x"])))

"If the second part of the keyword does not match an attribute, `nil` is added to the sequence."
(fact q5
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :div:not-an-attr) [nil])))

"If the first part of the keyword does not match the element, an empty sequence is returned."
(fact q6
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :p:id) [])))

"The attribute map is ignored when returning the contents of an element."
(fact q7
      (is (= (rq/query [:div {:id "x"} "foo" "bar"] :div) ["foo" "bar"])))

"If an element is a sequence (e.g., created with a `for`), we operate on all its elements and concatenate the results."
(fact q8
      (is (= (rq/query [:ul
                        [:li {:key -1}]
                        (for [i (range 3)]
                          [:li {:key i}])] :ul :li:key) [-1 0 1 2])))


[[:section {:title "Paths"}]]
"Every additional argument is applied to all the results of applying the previous argument.
For example, if we have a `:ul` element containing multiple `:li` elements, each containing a `:p` element and some text,
The path `:ul :li :p` will return a sequence of the strings."
(fact q-agg1
      (is (= (rq/query [:ul
                        [:li [:p "One"]]
                        [:li [:p "Two"]]
                        [:li [:p "Three"]]] :ul :li :p) ["One" "Two" "Three"])))

[[:section {:title "Classes"}]]
"When a path element starts with a dot (`.`), it is treated as a class matcher.
This means that it is expected to match against elements that have a `:class` attribute that includes the given class name.
For example, in the following example only \"Two\" has the class `selected`, hence it is the only one to be returned."
(fact q-cls1
      (is (= (rq/query [:ul
                        [:li {:class "other"} [:p "One"]]
                        [:li {:class "other selected"} [:p "Two"]]
                        [:li [:p "Three"]]] :ul :.selected :p) ["Two"])))

"The keyword can match at the same time against an element type and any number of classes.
For example, the keyword `:li.foo.bar` matches against `:li` elements that have both the `foo` and the `bar` classes."
(fact q-cls2
      (is (= (rq/query [:ul
                        [:li {:class "foo"} [:p "One"]]
                        [:li {:class "bar"} [:p "Two"]]
                        [:li {:class "bar foo"} [:p "Three"]]] :ul :li.foo.bar :p) ["Three"])))

"Classes can be used in conjunction with attributes."
(fact q-cls3
      (is (= (rq/query [:div
                        [:a {:href "foo.html"}]
                        [:a {:href "bar.html" :class "selected"}]
                        [:a {:href "baz.html"}]] :div :a.selected:href) ["bar.html"])))

"`reagent-query` identifies reagent's shorthand notation for classes, so vectors that begin with `:some-elem.some-class` are recognized as `:some-elem` with
`:class \"some-class\"."
(fact q-cls4
      (is (= (rq/query [:div
                        [:a.foo {:href "foo.html"}]
                        [:a.foo.selected {:href "bar.html"}]
                        [:a.foo {:href "baz.html"}]] :div :a.selected:href) ["bar.html"])))

[[:section {:title "Attribute Values"}]]
"`query` also accepts maps instead of keywords in the path (it uses [keyword-to-map](#keyword-to-map) to convert everything to maps).
For example, the selector `:a.selected:href` can be replaced with a map, as follows:"
(fact q-attr1
      (is (= (rq/query [:div
                        [:a {:href "foo.html"}]
                        [:a {:href "bar.html" :class "selected"}]
                        [:a {:href "baz.html"}]] :div {:elem :a
                                                       :classes #{"selected"}
                                                       :attr "href"}) ["bar.html"])))

"If a map selector contains a `:attr-vals` field, every key/value pair in the given map is checked against the attributes of the element under test."
(fact q-attr2
      (is (= (rq/query [:ul
                        [:li {:foo 1} "A"]
                        [:li {:foo 3 :bar 2} "B"]
                        [:li {:foo 1 :bar 2} "C"]]
                       :ul {:elem :li
                            :attr-vals {:foo 1 :bar 2}}) ["C"])))

"As shorthand, if a selector is a 2-element vector, its first element is [treated as a keyword](#keyword-to-map),
and its second argument is taken as the `:attr-vals` map.
The following example is exactly the same as the previous one, only that it uses the shorthand notation."
(fact q-attr3
      (is (= (rq/query [:ul
                        [:li {:foo 1} "A"]
                        [:li {:foo 3 :bar 2} "B"]
                        [:li {:foo 1 :bar 2} "C"]]
                       :ul [:li {:foo 1 :bar 2}]) ["C"])))

"Element names are not limited to keywords.
Elements can also be function names, in which case a map selector can be used to match them."
(fact q-attr4
      (is (= (rq/query [:div
                        [inc 1]
                        [dec 2]]
                       :div {:elem inc}) [1])))

[[:chapter {:title "find"}]]
"`find` is similar to `query`, only that it searches for its matches starting _anywhere in the hierarcy_,
not necessarily at the given root."

"For example, consider the following hiccup-like structure:"
(defonce find-example
  [:div
   [:ul
    [:li {:class "cart-item"}
     [:p "Keyboard"]]
    [:li {:class "cart-item current"}
     [:p "Mouse"]]
    [:li {:class "cart-item"}
     [:p "Monitor"]]]])

"We can list all items by simply finding all `:p` elements that are direct descendants of `:.cart-item`s."
(fact find1
      (is (= (rq/find find-example :.cart-item :p) ["Keyboard" "Mouse" "Monitor"])))

"Similarly, if we only want the current item we can look for it directly:"
(fact find2
      (is (= (rq/find find-example :.current :p) ["Mouse"])))

[[:chapter {:title "mock-change-event"}]]
"`mock-change-event` is a conveniece function that creates a mock `:on-change` event.
The function takes as parameter a new value, and generates a Javascript object that has a single member: `target`,
which by itself is a Javascript object with one field: `value`, containing the given value."
(fact mce1
      (let [ev (rq/mock-change-event "val")]
        (is (= (.-target.value ev) "val"))))

"`mock-change-event` takes an optional second argument as the attribute in the target to which the value should be set.
This is useful for input elements for which the content is mapped to attributes other than _value_."
(fact mce2
      (let [ev (rq/mock-change-event true "checked")]
        (is (= (.-target.checked ev) true))))

[[:chapter {:title "Under the Hood"}]]
[[:section {:title "keyword-to-map"}]]
"Under the hood, `reagent-query` paths consist of maps with the following optional fields:
1. `:elem`: The element name to match
2. `:classes`: A set of strings, each needs to be a class in the element.
3. `:attr`: If present, the result is the contents of the specified attribute, and not the contents of the element.
4. `:attr-vals`: A map of attributes with their respective values. If present, all attribute values must match."

"`keyword-to-map` converts a keyword to such a map, using the following rules."

"By default, a keyword maps into a map with an `:elem` field, of that value converted to a string."
(fact keyword-to-map1
      (is (= (:elem (rq/keyword-to-map :my-elem)) :my-elem)))

"When the keyword contains dots (`.`), the part of its name that precedes the first dot is considered the `:elem`, and the other parts are taken as the `:classes` set."
(fact keyword-to-map2
      (let [m (rq/keyword-to-map :foo.bar.baz)]
        (is (= (:elem m) :foo))
        (is (= (:classes m) #{"bar" "baz"}))))

"Omitting the element makes it `nil`."
(fact keyword-to-map3
      (let [m (rq/keyword-to-map :.bar.baz)]
        (is (= (:elem m) nil))
        (is (= (:classes m) #{"bar" "baz"}))))

"When the keyword contains a colon (`:`), the right-hand-side of the colon is taken as the `:attr` field."
(fact keyword-to-map4
      (let [m (rq/keyword-to-map :foo:quux)]
        (is (= (:elem m) :foo))
        (is (= (:attr m) "quux"))))

[[:section {:title "all-elems"}]]
"The `all-elems` function takes a hiccup-like vector and returns all its sub-components that represent elements, at any nesting level."

"For a simple element (with no nested elements), `all-elems` returns the element itself as a single result."
(fact all-elems1
      (is (= (rq/all-elems [:hr]) [[:hr]])))

"If child elements exist, they appear in the returned sequence after the root."
(fact all-elems2
      (let [example [:ul
                     [:li "One"]
                     [:li "Two"]
                     [:li "Three"]]
            all (rq/all-elems example)]
        (is (= (first all) example))
        (is (= (rest all) (rest example)))))

"Child elements that do not represent HTML elements (i.e., not vectors) are ignored."
(fact all-elems3
      (is (= (count (rq/all-elems [:h1 "Hello" "World"])) 1)))

"`all-elems` works recursively.
Consider for example the following structure:"
(defonce example1
  [:div
   [:h1 "The Title"]
   [:ul
    (for [[i s] (map-indexed vector ["Zero" "One" "Two"])]
      [:li {:key i}
       [:p s]])]])

"Applying `all-elems` on this structure will emit 1 `:div`, 1 `:h1`, 1 `:ul`, 3 `:li` and 3 `:p` elements."
(fact all-elems4
      (is (= (count (rq/all-elems example1)) (+ 1 1 1 3 3))))
