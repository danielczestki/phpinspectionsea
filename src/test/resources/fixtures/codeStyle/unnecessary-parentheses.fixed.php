<?php

    require_once 'whatever.php';
    $x = $x + 1;
    $x = file_get_contents();
    $x = clone $object;

    $x = ($x + 1) / 2;

    (new stdClass())->with('data');
    (new stdClass())->prop;

    ($x ?: $y)->with('data');
    ($x ?: $y)->prop;
    ($x ?? $y)->with('data');
    ($x ?? $y)->prop;

    (clone $stdClass)->with('data');
    (clone $stdClass)->prop;

    ($x->callableProperty)();
    ($x->getCallableProperty())();

    /* __invoke */
    (new stdClass())();
    (new self)();
    (clone $stdClass)();

    $mixedIncludeReturn = (include __DIR__ . '/foo.php');

    (function(){})();

    $x = !($first = $x->prop)->with('data');