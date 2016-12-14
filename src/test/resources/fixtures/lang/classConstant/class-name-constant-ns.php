<?php

namespace PhpInspections\EAExtended;

use PhpInspections\EAExtended\Prerequisites as PrerequisitesAlias;
use PhpInspections\EAExtended\Prerequisites;
use \stdClass as stdClassAlias;

/** @depends("Prerequisites") */
class Action {
    public function patterns() {
        $dependencies = [
            'PrerequisitesAlias',
            'stdClassAlias',
            'Prerequisites',

            <weak_warning descr="Perhaps this can be replaced with \stdClass::class">'\stdClass'</weak_warning>,
            <weak_warning descr="Perhaps this can be replaced with \stdClass::class">'stdClass'</weak_warning>,
            <weak_warning descr="Perhaps this can be replaced with \PhpInspections\EAExtended\Prerequisites::class">'\PhpInspections\EAExtended\Prerequisites'</weak_warning>,
            <weak_warning descr="Perhaps this can be replaced with \PhpInspections\EAExtended\Prerequisites::class">'PhpInspections\EAExtended\Prerequisites'</weak_warning>,
            <weak_warning descr="Perhaps this can be replaced with \PhpInspections\EAExtended\Prerequisites::class">"\\PhpInspections\\EAExtended\\Prerequisites"</weak_warning>,
            <weak_warning descr="Perhaps this can be replaced with \PhpInspections\EAExtended\Prerequisites::class">"PhpInspections\\EAExtended\\Prerequisites"</weak_warning>,
        ];
    }

    public function falsePositives() {
        $x  = '';
        $x .= '\stdClass';

        $isExists = class_exists('\stdClass');

        return [
            '\stdClass' . $x,
            "\\stdClass{$x}",
            $x === '\stdClass',
        ];
    }
}