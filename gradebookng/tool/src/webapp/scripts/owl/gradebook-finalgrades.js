/**************************************************************************************
 *                    Gradebook Final Grades Javascript
 *************************************************************************************/

/**************************************************************************************
 * A GradebookSpreadsheet to encapsulate all the grid features
 */
function GradebookSpreadsheet($spreadsheet) {
  this.$spreadsheet = $spreadsheet;
  this.$table = $("#gradebookGradesTable");

  // no students or grade items, nothing to do
  if (this.$table.length === 0) {
	  return;
  }

  // all the Final Grade cell models keyed on studentUuid
  this._GRADE_CELLS = {};

  // set it all up
  this.setupGradeItemCellModels();

  var self = this;
  // these things are less important, so can push off the
  // critical path of the page load
  this.onReady(function() {
    self.setupKeyboadNavigation();
    self.setupRowSelector();
    self.setupStudentFilter();
    self.setupMenusAndPopovers();
  });

  this.onReady(function() {
    self.setupConnectionPoll();
  })

  this.ready();
};

GradebookSpreadsheet.prototype.initTable = function()
{
    GradebookSpreadsheet.prototype._callbacks = [];

    this._GRADE_CELLS = {};
    this.setupGradeItemCellModels();

    var self = this;
    self.setupKeyboadNavigation();
    self.setupRowSelector();
    self.setupStudentFilter();
    self.setupMenusAndPopovers();

    self.setupConnectionPoll();

    this.ready();
};


GradebookSpreadsheet.prototype.getCellModelForWicketParams = function(wicketExtraParameters) {
    var extraParameters = {};

    if (!wicketExtraParameters) {
      return;
    }

    wicketExtraParameters.map(function(o, i) {
      extraParameters[o.name] = o.value;
    });

    return this.getCellModelForStudent(extraParameters.studentUuid);
};


GradebookSpreadsheet.prototype.setupGradeItemCellModels = function() {
  var self = this;

  self.$table.find("> thead > tr.gb-headers > th").each(function(cellIndex, cell) {
    var $cell = $(cell);

    var model = new GradebookHeaderCell($cell, self);

  });

  self.$table.on("focus", "td, th", function(event) {
    // lazy load model
    self.getCellModel($(this));
  });

  self.$table.on("focus", "td.gb-final-grade :text", function(event) {
    // lazy load model
    self.getCellModel($(event.target).closest("td"));
  });
};


GradebookSpreadsheet.prototype.setupKeyboadNavigation = function() {
  var self = this;

  self.$table.
    on("keydown", function(event) {
      return self.onKeydown(event);
    });
};


GradebookSpreadsheet.prototype.onKeydown = function(event) {
  var self = this;

  var $eventTarget = $(event.target);

  if (!$eventTarget.is("td,th")) {
    return true;
  }

  var isEditableCell = $eventTarget.hasClass("gb-final-grade");


  // arrow left 37 (DISABLE TAB FOR NOW || tab 9 + SHIFT)
  if (event.keyCode === 37) { // || (event.shiftKey && event.keyCode == 9)) {
    self.navigate(event, event.target, "left");

  // arrow up 38
  } else if (event.keyCode === 38) {
    self.navigate(event, event.target, "up");

  // arrow right 39 (DISABLE TAB FOR NOW || tab 9)
  } else if (event.keyCode === 39) { // || event.keyCode == 9) {
    self.navigate(event, event.target, "right");

  // arrow down 40
  } else if (event.keyCode === 40) {
    self.navigate(event, event.target, "down");

  // return 13
  } else if (isEditableCell && event.keyCode === 13) {
    event.preventDefault();
    self.getCellModel($eventTarget).enterEditMode(event.keyCode);

  // 0-9 48-57 and keypad 0-9 96-105
  } else if (isEditableCell &&
      ((event.keyCode >= 48 && event.keyCode <= 57) || (event.keyCode >= 96 && event.keyCode <= 105))) {
    event.preventDefault();
    self.getCellModel($eventTarget).enterEditMode(event.keyCode);

  // DEL 8
  } else if (isEditableCell && event.keyCode === 8) {
    event.preventDefault();
    // TODO: no clear mechanism yet
    //self.getCellModel($eventTarget).clear();

  // ESC 27
  } else if (event.keyCode === 27) {
    event.preventDefault();
    self.$table.find('[data-toggle="popover"]').popover("hide");
  }
};


GradebookSpreadsheet.prototype.navigate = function(event, fromCell, direction, enableEditMode) {
  var self = this;

  var $cell = $(fromCell);
  var aCell = self.getCellModel($cell);

  var $row = aCell.getRow();

  var $targetCell;

  if (direction === "left") {
    if ($cell.index() > 0) {
      event.preventDefault();
      event.stopPropagation();

      $targetCell = $cell.prevAll(":visible:first");

    } else {
      fromCell.focus();
      return true;
    }
  } else if (direction === "right") {
    event.preventDefault();
    event.stopPropagation();

    if ($cell.index() < $row.children().last().index()) {
      $targetCell = $cell.nextAll(":visible:first");
    } else {
      fromCell.focus();
      if (fromCell.data("_pendingReplacement")) {
        fromCell.data("model")._focusAfterSaveComplete = true;
      }
      return true;
    }
  } else if (direction === "up") {
    // can we go up a row inside the tbody
    if ($row.index() > 0) {
      event.preventDefault();
      event.stopPropagation();

      var $targetRow = aCell.getRow().prevAll(":visible:first");

      if ($targetRow.length === 0) {
        // all rows above are hidden! Jump to the header
        $targetRow = self.$table.find("> thead > tr.gb-headers");
      }

      $targetCell = $targetRow.find("> *:nth-child("+($cell.index()+1)+")");

    // can we go up a row to the thead
    } else if ($row.index() === 0 && $row.parent().is("tbody")) {
      event.preventDefault();
      event.stopPropagation();

      $targetCell = self.$table.find("> thead > tr.gb-headers").
                      find("> *:nth-child("+($cell.index()+1)+")");

    // or are we at the top!
    } else {
      fromCell.focus();
    }
  } else if (direction === "down") {
    if ($row.parent().is("thead")) {
      event.preventDefault();
      event.stopPropagation();

      $targetCell = self.$table.find("> tbody > tr:visible:first").
                      find("> *:nth-child("+($cell.index()+1)+")");
    } else if ($row.index() < $row.siblings().last().index()) {
      event.preventDefault();
      event.stopPropagation();

      $targetCell = aCell.getRow().nextAll(":visible:first").
                                      find("> *:nth-child("+($cell.index()+1)+")");

    } else {
      fromCell.focus();
      if (fromCell.data("_pendingReplacement")) {
        fromCell.data("model")._focusAfterSaveComplete = true;
      }
    }
  }

  // stay in "edit mode" if the next cell is editable
  if (enableEditMode && $targetCell && $cell !== $targetCell) {
    var targetCellModel = self.getCellModel($targetCell);
    if (targetCellModel.isEditable()) {
      targetCellModel.enterEditMode();
    } else {
      $targetCell.focus();
    }
  } else if ($targetCell && $targetCell.is(":visible")) {
    $targetCell.focus();
  } else {
    // ensure the table retains focus to facilitate continuation of keyboard navigation
    aCell._focusAfterSaveComplete = true;
  }

  return false;
};


GradebookSpreadsheet.prototype.ensureCellIsVisible = function($cell) {
  var self= this;

  // check input is visible on y-scroll
  if ($cell.parent().parent().prop("tagName") === "TBODY") {
    var $header = self.getHeader();
    var headerBottomPosition = $header[0].offsetTop + $header[0].offsetHeight;
    if ($cell[0].offsetTop < headerBottomPosition) {
      $(window).scrollTop($(window).scrollTop() - (headerBottomPosition - ($cell[0].offsetTop - $cell.height())));
    }
  }
};

GradebookSpreadsheet.prototype.getCellModelForStudent = function(studentUuid)
{
  if (this._GRADE_CELLS.hasOwnProperty(studentUuid)) {
    return this._GRADE_CELLS[studentUuid];
  }

  var $cell = this.$table.find("> tbody td.gb-final-grade[data-studentuuid='"+studentUuid+"']:first");
  var $header = this.$table.find("> thead > tr:last .gb-final-grade");
  cellModel = new GradebookEditableCell($cell, this.getCellModel($header), this);

  this._GRADE_CELLS[studentUuid] = cellModel;

  return this._GRADE_CELLS[studentUuid];
};


GradebookSpreadsheet.prototype.getCellModel = function($cell) {
  if ($cell.data("model")) {
    return $cell.data("model");
  }

  if ($cell.data("studentuuid")) {
    return this.getCellModelForStudent($cell.data("studentuuid"));
  }

  var headerModel = this.getCellModel(this.$table.find("> thead > tr:last > th:eq(" + $cell.index() + ")"));

  return new GradebookBasicCell($cell, headerModel, this);
};


GradebookSpreadsheet.prototype.handleInputReturn = function(event, $cell) {
  this.navigate(event, $cell, "down", true);
};


GradebookSpreadsheet.prototype.handleInputArrowKey = function(event, $cell) {
  if (event.keyCode === 37) {
    this.navigate(event, $cell, "left", true);
  } else if (event.keyCode === 38) {
    this.navigate(event, $cell, "up", true);
  } else if (event.keyCode === 39) {
    this.navigate(event, $cell, "right", true);
  } else if (event.keyCode === 40) {
    this.navigate(event, $cell, "down", true);
  }
  return false;
};


GradebookSpreadsheet.prototype.handleInputTab = function(event, $cell) {
  if (event.shiftKey) {
    this.navigate(event, $cell, "left", true);
  } else {
    this.navigate(event, $cell, "right", true);
  }
};


GradebookSpreadsheet.prototype.getHeader = function() {
  return this.$table.find("> thead", "> tr");
};

GradebookSpreadsheet.prototype.find = function() {
  return this.$spreadsheet.find.apply(this.$spreadsheet, arguments);
}

GradebookSpreadsheet.prototype.highlightRow = function($row) {
  this.$spreadsheet.find(".gb-highlighted-row").removeClass("gb-highlighted-row");
  $row.addClass("gb-highlighted-row");
};


GradebookSpreadsheet.prototype.setupRowSelector = function() {
  this.$table.on("click", '.gb-row-selector', function() {
    $(this).next().focus();
  });
};

GradebookSpreadsheet.prototype.setupStudentFilter = function() {
  var self = this;

  self.$table.on("keydown", "#studentFilterInput", function(event)
  {
      if (event.keyCode === 13)  // Enter/return
      {
          event.preventDefault();
          event.stopPropagation();
          self.$table.find(".studentFilterButton").trigger("click");
      }
  });

  self.$table.on("keydown", "#studentNumberFilterInput", function(event)
  {
      if (event.keyCode === 13)  // Enter/return
      {
          event.preventDefault();
          event.stopPropagation();
          self.$table.find(".studentNumberFilterButton").trigger("click");
      }
  });

};


GradebookSpreadsheet.prototype.setupMenusAndPopovers = function() {
  var self = this;

  self._popovers = [];

  function hideAllPopovers() {
    $.each(self._popovers, function(i, popover) {
      popover.popover("hide");
    });
    self._popovers = [];
  };

  self.popoverClicked = false;

  self.enablePopovers(self.$table);

  self.$spreadsheet.on("focus", '[data-toggle="popover"]', function(event) {
    if (self.suppressPopover) {
      self.suppressPopover = false;
      return;
    }

    hideAllPopovers();

    $(event.target).data("popoverShowTimeout", setTimeout(function() {
      $(event.target).popover('show');
      self._popovers.push($(event.target));
    }, 500));
  });

  self.$spreadsheet.on("click", ".popover", function(event) {
    self.popoverClicked = true;
  }).on("click", ":not(.popover)", function(event) {
    setTimeout(function() {
      hideAllPopovers();
    }, 100);
  }).on("click", ".popover .gb-popover-close", function(event) {
    var $link = $(this);
    var $cellToFocus;

    if ($link.data("studentuuid")) {
      var cell = self.getCellModelForStudent($link.data("studentuuid"));
      $cellToFocus = cell.$cell;
    } else {
      $cellToFocus = $link.closest("td,th");
    }

    hideAllPopovers();
    $cellToFocus.focus();
  }).on("click", ".popover .gb-revert-score", function(event) {
    event.preventDefault();
    var $popover = $(event.target).closest(".popover");
    var $close = $popover.find(".gb-popover-close");
    var cell = self.getCellModelForStudent($close.data("studentuuid"));
    cell._focusAfterSaveComplete = true;
    cell.$input.trigger("revertscore.sakai");
    hideAllPopovers();
  });;

  // close the dropdown if the user navigates away from it
  self.$spreadsheet.find(".btn-group").on("shown.bs.dropdown", function(event) {
    var $btnGroup = $(event.target);

    function handleDropdownItemBlur(blurEvent) {
      if ($(blurEvent.relatedTarget).closest(".btn-group.open").length == 0) {
        // Firefox will only offer a blurEvent.relatedTarget if the item can be focussed
        // and links will only be included in the tab index if the user's accessibility
        // configuration has this option enabled (e.g. accessibility.tabfocus option).
        // Instead, delay hiding the menu (0.5s is enough) to allow any click events to
        // hit the link before we force close the menu.
        setTimeout(function() {
          if ($btnGroup.is(".open")) {
            $btnGroup.find(".btn.dropdown-toggle").dropdown("toggle");
          }
        }, 500);
      }
    };

    $btnGroup.find(".btn.dropdown-toggle").on("mousedown", function(mouseDownEvent) {
      if ($(mouseDownEvent.target).closest(".btn-group.open").length > 0) {
        mouseDownEvent.stopPropagation();
        $(mouseDownEvent.target).focus();
      }
    })

    $btnGroup.find("ul.dropdown-menu li a").on("mousedown", function(mouseDownEvent) {
      mouseDownEvent.stopPropagation();
      $(mouseDownEvent.target).focus();
    })

    $btnGroup.find(".btn.dropdown-toggle, ul.dropdown-menu li a").on("blur", handleDropdownItemBlur);

    $btnGroup.one("hidden.bs.dropdown", function() {
      $btnGroup.find(".btn.dropdown-toggle, ul.dropdown-menu li a").off("blur", handleDropdownItemBlur);
    });
  });
};


GradebookSpreadsheet.prototype.enablePopovers = function($target) {
  var self = this;
  var $popovers = $target.find('[data-toggle="popover"]');

  $popovers.popover("destroy");

  $popovers.popover().blur(function(event) {
    clearTimeout($(event.target).data("popoverShowTimeout"));
    $(event.target).data("popoverHideTimeout", setTimeout(function() {
      if (!self.popoverClicked) {
        $(event.target).popover("hide");
      }
    }, 100));
  }).on("hidden.bs.popover", function() {
    self.popoverClicked = false;
  }).on("shown.bs.popover", function(event) {
    var $popover = $(this).data("bs.popover").$tip;
    var bottomMostPoint = $popover.position().top + $popover.outerHeight();
    if (bottomMostPoint > self.$spreadsheet[0].offsetHeight) {
      self.$spreadsheet[0].scrollTop = bottomMostPoint - self.$spreadsheet[0].offsetHeight + 20;
    }
  });

  // Ensure the popover doesn't get in the way of the dropdown menu
  $popovers.find('.btn-group').on("shown.bs.dropdown", function() {
    var $popover = $(this).closest('[data-toggle="popover"]');
    if ($popover.length > 0) {
      clearTimeout($popover.data("popoverShowTimeout"));
      $popover.popover("hide");
    }
  });
};


GradebookSpreadsheet.prototype.ready = function() {
  this.$spreadsheet.data("initialized", true);//.trigger("ready.gradebookng");
  $.each(GradebookSpreadsheet.prototype._callbacks, function(i, callback) {
    callback();
  });
  GradebookSpreadsheet.prototype._callbacks = null;
}

GradebookSpreadsheet.prototype._callbacks = [];

GradebookSpreadsheet.prototype.onReady = function(callback) {
  if (this.$spreadsheet.data("initialized") == true) {
    setTimeout(function() { callback(); });
  } else {
    GradebookSpreadsheet.prototype._callbacks.push(callback);
  }
};


GradebookSpreadsheet.prototype.setupCell = function(cellId, studentUuid) {
  var cellModel = this.getCellModelForStudent(studentUuid);
  cellModel.handleSaveComplete(cellId)
};


GradebookSpreadsheet.prototype.findVisibleStudentBefore = function(studentUuid) {
  var $cell = this.$spreadsheet.find(".gb-student-cell[data-studentuuid='"+studentUuid+"']");
  var $row = $cell.closest("tr");

  var $targetRow = $row.prevAll(":visible:first");
  if ($targetRow.length > 0) {
    return $targetRow.find(".gb-student-cell");
  } else {
    return false;
  }
};


GradebookSpreadsheet.prototype.findVisibleStudentAfter = function(studentUuid) {
  var $cell = this.$spreadsheet.find(".gb-student-cell[data-studentuuid='"+studentUuid+"']");
  var $row = $cell.closest("tr");

  var $targetRow = $row.nextAll(":visible:first");
  if ($targetRow.length > 0) {
    return $targetRow.find(".gb-student-cell");
  } else {
    return false;
  }
}

GradebookSpreadsheet.prototype.refreshCourseGradeForStudent = function(studentUuid) {
  // cell has been updated, so need to refresh the course grade in the fixed column
  // on the off chance the grade has changed
  // KILLDUPES??? be careful deleting this one....

  /*var $studentNameCell = this.$table.find(".gb-student-cell[data-studentuuid='"+studentUuid+"']");
  var $courseGradeCell = $studentNameCell.closest("tr").find(".gb-course-grade");

  var $fixedColumnStudentNameCell = this.$fixedColumns.find(".gb-student-cell[data-studentuuid='"+studentUuid+"']");
  var $fixedColumnCourseGradeCell = $fixedColumnStudentNameCell.closest("tr").find(".gb-course-grade");

  var courseGrade = this._cloneCell($courseGradeCell).html();
  $fixedColumnCourseGradeCell.html(courseGrade);
  $fixedColumnCourseGradeCell.addClass("gb-score-dynamically-updated");*/

  this.$spreadsheet.find(".gb-score-dynamically-updated").removeClass("gb-score-dynamically-updated", 1000);
};


GradebookSpreadsheet.prototype.refreshStudentSummary = function() {
  var $labelCount = this.$spreadsheet.find(".gb-student-summary-counts .visible");

  $labelCount.html(this.$table.find("tbody tr:visible").length);
};

GradebookSpreadsheet.prototype.positionModalAtTop = function($modal) {
  // position the modal at the top of the viewport
  // taking into account the current scroll offset
  $modal.css('top', 30 + $(window).scrollTop() + "px");
};

GradebookSpreadsheet.prototype.setLiveFeedbackAsSaving = function() {
  var $liveFeedback = this.$spreadsheet.closest("#gradebookSpreadsheet").find(".gb-live-feedback");
  $liveFeedback.show();
  var $span = $liveFeedback.children().first();
  $span.removeClass();
  $span.html($span.data("saving-message"));
};


/*************************************************************************************
 * AbstractCell - behaviour inherited by all cells
 */
var GradebookAbstractCell = {
  setupCell: function($cell) {
    var self = this;
    self.$cell = $cell;
    $cell.data("model", this);
    $cell.on("focus", function(event) {
      self.gradebookSpreadsheet.ensureCellIsVisible($(event.target));
      self.gradebookSpreadsheet.highlightRow(self.getRow());
    });
  },
  getRow: function() {
    return this.$cell.closest("tr");
  },
  show: function() {
    this.$cell.show();
  },
  hide: function() {
    this.$cell.hide();
  }
};

GradebookSpreadsheet.prototype.setupConnectionPoll = function() {
  this.ping = new ConnectionPoll($("#gbConnectionTimeoutFeedback"));
};

GradebookSpreadsheet.prototype.setupSectionStats = function()
{
    this.stats = new SectionStats();
}

/*************************************************************************************
 * GradebookEditableCell - behaviour for editable cells
 */
function GradebookEditableCell($cell, header, gradebookSpreadsheet) {
  this.header = header;
  this.gradebookSpreadsheet = gradebookSpreadsheet;
  this.$spreadsheet = gradebookSpreadsheet.$spreadsheet;

  this.setupEditableCell($cell);
};


GradebookEditableCell.prototype = Object.create(GradebookAbstractCell);

GradebookEditableCell.prototype.setupEditableCell = function($cell) {
  this.$input = $cell.find("input.gb-editable-final-grade:first");

  this.setupCell($cell);

  this.$cell.data("initialValue", null);
  this.$cell.data("wicket_input_initialized", false).removeClass("gb-cell-editing");
  this.$cell.data("wicket_label_initialized", true);

  this.setupInput();
};

GradebookEditableCell.prototype.isEditable = function() {
  return true;
};

GradebookEditableCell.prototype.setupInputKeyboardNavigation = function() {
  var self = this;
  self.$input.on("keydown", function(event) {
    // Return 13
    if (event.keyCode == 13) {
      // first blur the $input to trigger a change event
      self.$input.trigger("blur");

      // ask the spreadsheet to navigate based on a return key action
      self.gradebookSpreadsheet.handleInputReturn(event, self.$cell);
    // ESC 27
    } else if (event.keyCode == 27) {
      self.$cell.focus();
      self._focusAfterSaveComplete = true;

    // arrow keys
    } else if (event.keyCode >= 37 && event.keyCode <= 40) {
      self.gradebookSpreadsheet.handleInputArrowKey(event, self.$cell);

    // TAB 9
    } else if (event.keyCode == 9) {
      self.gradebookSpreadsheet.handleInputTab(event, self.$cell);
    }
  });
};

GradebookEditableCell.prototype.setupInput = function() {
  var self = this;

  if (self.$cell.data("wicket_input_initialized")) {
    return;
  }

  function prepareForEdit(event) {
    self.$cell.addClass("gb-cell-editing");

    //strip any symbols and put it back into the input field
    var inputVal = strip(self.$input.val());
    self.$input.val(inputVal);

    self.$cell.data("originalValue", self.$input.val());

    var withValue = self.$cell.data("initialValue");

    if (withValue != null && withValue != "") {
      self.$input.val(withValue);
    } else {
      self.$input.select();
    }

    // ensure row is highlighted if triggered from click
    self.gradebookSpreadsheet.highlightRow(self.getRow());
  }

  function completeEditing(event) {
    self.$cell.removeClass("gb-cell-editing");
    //self.$cell.find(".gb-out-of").remove();
    self.$cell.data("initialValue", null);

    // In Chrome, IE, the "change" event is only triggered after direct user
    // changes to the input and not after jQuery.val().  So need to ensure
    // "change" is triggered once when a user or programmatic change is detected.
    // To get around this, we use a custom event "scorechanged" and trigger this
    // manually; a Wicket behaviour is bound to this custom event and handles the
    // the update in the Wicket backend.
    if (self.$cell.data("originalValue") != self.$input.val()) {
      self.$cell.data("_pendingReplacement", true);
      self.$input.trigger("scorechange.sakai");
    }
  }

  function strip(value) {
	  return value.replace('%','');
  }

  self.$input.off("focus", prepareForEdit).on("focus", prepareForEdit);
  self.$input.off("blur", completeEditing).on("blur", completeEditing);

  self.setupInputKeyboardNavigation();

  self.$cell.data("wicket_input_initialized", true);
};


GradebookEditableCell.prototype.getHeaderCell = function() {
  return this.header.$cell;
};


GradebookEditableCell.prototype.getOutOfLabel = function() {
  return this.header.$cell.find(".gb-total-points").data("outof-label");
};


GradebookEditableCell.prototype.enterEditMode = function(keyCode) {
  var self = this;

  var initialValue = "";

  if (keyCode && typeof keyCode == "number") {
    // only buffer 0-9 key strokes
    if (keyCode >= 48 && keyCode <= 57) {
      initialValue = keyCode - 48;
    } else if (keyCode >= 96 && keyCode <= 105) {
      initialValue = keyCode - 96;
    }
  }

  self.$cell.data("initialValue", initialValue + "");
  self.$input.focus();
};


GradebookEditableCell.prototype.getWicketAjaxLabel = function() {
    return this.$cell.find("span[id^='label']");
};

GradebookEditableCell.prototype.getStudentName = function() {
  return this.$cell.closest("tr").find(".gb-student-cell").text().trim();
};


GradebookEditableCell.prototype.handleBeforeSave = function() {
  this.$cell.addClass("gb-cell-saving");
  this.gradebookSpreadsheet.setLiveFeedbackAsSaving();
};


GradebookEditableCell.prototype.handleSaveComplete = function(cellId) {
  this.handleWicketCellReplacement(cellId);
  // ensure fixed headers are aligned correctly after save, as vertical scroll
  // may change as messages are added/removed from above the grade table
  /*setTimeout(function() {
    // take this off the critical path as we don't want any errors on
    // the document scroll to stop the spreadsheet working
    $(document).trigger("scroll");
  });*/
};


GradebookEditableCell.prototype.handleWicketCellReplacement = function(cellId) {
  //bind a timeout to the successful save. An easing would be nice
  $(".grade-save-success").removeClass("grade-save-success", 1000);

  this.setupEditableCell($("#" + cellId));

  //re-enable popover?
  if (this.$cell.is('[data-toggle="popover"]')) {
    this.gradebookSpreadsheet.enablePopovers(this.$cell);
  }

  if (this._focusAfterSaveComplete) {
    this.$cell.focus();
    this._focusAfterSaveComplete = false;
  }
};


/**************************************************************************************
 * GradebookBasicCell basic cell with basic functions
 */
function GradebookBasicCell($cell, header, gradebookSpreadsheet) {
  this.header = header;
  this.gradebookSpreadsheet = gradebookSpreadsheet;

  this.setupCell($cell);
};


GradebookBasicCell.prototype = Object.create(GradebookAbstractCell);


GradebookBasicCell.prototype.isEditable = function() {
  return false;
};


/**************************************************************************************
 * GradebookHeaderCell basic header cell with basic functions
 */
function GradebookHeaderCell($cell, gradebookSpreadsheet) {
  this.gradebookSpreadsheet = gradebookSpreadsheet;

  this.setupCell($cell);

  this.setColumnKey();
  this.truncateTitle();
  this.setupTooltip();
};


GradebookHeaderCell.prototype = Object.create(GradebookAbstractCell);


GradebookHeaderCell.prototype.isEditable = function() {
  return false;
};


GradebookHeaderCell.prototype.setColumnKey = function() {
  var self = this;

  var columnKey;
  if (self.$cell.hasClass("gb-grade-item-column-cell")) {
    columnKey = self.$cell.find("[data-assignmentid]").data("assignmentid");
  } else if (self.$cell.hasClass("gb-category-item-column-cell")) {
    columnKey = "category_" + self.$cell.find(".gb-title").text().trim();
  } else if (self.$cell.find(".gb-title").length > 0) {
    columnKey = self.$cell.find(".gb-title").text().trim();
  } else {
    columnKey = self.$cell.find("span:first").text().trim();
  }
  self.columnKey = columnKey;

  return columnKey;
}


GradebookHeaderCell.prototype.getTitle = function() {
  if (self.$cell.hasClass("gb-grade-item-column-cell")) {
    return this.$cell.find(".gb-title span[title]").attr("title");
  } else {
    throw "getTitle not supported yet";
  }
};


GradebookHeaderCell.prototype.truncateTitle = function() {
  var self = this;

  if (self.$cell.hasClass("gb-grade-item-column-cell")) {
    var $title = self.$cell.find(".gb-title");
    var targetHeight = $title.height();
    if ($title[0].scrollHeight > targetHeight) {
      var $titleText = $title.find("span[title]");
      var words = $titleText.text().split(" ");

      while (words.length > 1) {
        words = words.slice(0, words.length - 1); // drop a word
        $titleText.html(words.join(" ") + "&hellip;");
        if ($title[0].scrollHeight <= targetHeight) {
          break;
        }
      }
    }

  }
};


GradebookHeaderCell.prototype.show = function() {
  this.$cell.show();

  if (this.$categoryCell) {
    this.$categoryCell.show();
    var newColspan = parseInt(this.$categoryCell.attr("colspan")) + 1;
    this.$categoryCell.attr("colspan", newColspan);
    this.$categoryCell.show();
  }
};


GradebookHeaderCell.prototype.hide = function() {
  this.$cell.hide();

  if (this.$categoryCell) {
    var newColspan = parseInt(this.$categoryCell.attr("colspan")) - 1;
    this.$categoryCell.attr("colspan", newColspan);
    if (newColspan == 0) {
      this.$categoryCell.hide();
    }
  }
};


GradebookHeaderCell.prototype.setupTooltip = function() {
  if (this.$cell.hasClass("gb-grade-item-column-cell")) {
    var $title = this.$cell.find(".gb-title > a");
    var tooltip = $title.attr("title");

    tooltip += " (" + this.getCategory() + ")";

    this.$cell.attr("title", tooltip);

    // remove the $title[@title] so it doesn't conflict with the outer title
    $title.removeAttr("title");
  }
};

/**************************************************************************************
 * GradebookWicketEventProxy - proxy any Wicket events to the Gradebook Spreadsheet
 */

GradebookWicketEventProxy = {
  updateGradeItem: {
    handlePrecondition: $.noop,
    handleBeforeSend: function(cellId, attrs, jqXHR, settings) {
      var model = sakai.gradebookng.spreadsheet.getCellModelForWicketParams(attrs.ep);
      model.handleBeforeSave && model.handleBeforeSave();
    },
    handleSuccess: $.noop,
    handleFailure: $.noop,
    handleComplete: function(cellId, attrs, jqXHR, textStatus) {
      var model = sakai.gradebookng.spreadsheet.getCellModelForWicketParams(attrs.ep);
      model.handleSaveComplete && model.handleSaveComplete(cellId);
    }
  },
  revertGradeItem: {
    handleComplete: function(cellId, attrs, jqXHR, textStatus) {
      var model = sakai.gradebookng.spreadsheet.getCellModelForWicketParams(attrs.ep);
      model.handleWicketCellReplacement && model.handleWicketCellReplacement(cellId);
    }
  }
};

/*********************************************
 * No-op for GbModalWindow compatibility
 ********************************************/
var GradebookGradeSummaryUtils = {
  clearBlur: function() {
    /*$(".gb-blur").removeClass("gb-blur");*/
  }
};

/**************************************************************************************
 *                    Connection Poll Javascript
 *************************************************************************************/
function ConnectionPoll($message) {
    this.PING_INTERVAL = 1000*30; // 30 seconds
    this.PING_TIMEOUT = 1000*10; // 10 seconds
    this.PING_URL = "/direct/gbng/ping";

    this.$message = $message;

    this.poll();
};


ConnectionPoll.prototype.poll = function() {
  var self = this;

  self._interval = setInterval(function() {
    self.ping();
  }, self.PING_INTERVAL);
};


ConnectionPoll.prototype.ping = function() {
  $.ajax({
    type: "GET",
    url: this.PING_URL,
    data: {
      auto: true // indicate that the request is automatic, not from a user action
    },
    timeout: this.PING_TIMEOUT,
    cache: false,
    success: $.proxy(this.onSuccess, this),
    error: $.proxy(this.onTimeout, this)
  });
};

ConnectionPoll.prototype.onTimeout = function() {
  this.$message.show();
};

ConnectionPoll.prototype.onSuccess = function() {
  this.$message.hide();
};


/**************************************************************************************
 * Let's initialize our GradebookSpreadsheet
 */
var start;
$(function() {
  start = Date.now();
  sakai.gradebookng = {
    spreadsheet: new GradebookSpreadsheet($("#gradebookGrades"))
  };
});

function reinitSpreadsheet()
{
    sakai.gradebookng.spreadsheet.$spreadsheet = $("#gradebookGrades");
    sakai.gradebookng.spreadsheet.$table = $("#gradebookGradesTable");
    sakai.gradebookng.spreadsheet.initTable();
}



