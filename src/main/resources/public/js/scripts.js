/*
 *   Copyright 2018 Peter Kiss and David Fonyo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

var param_names,objective_relation_types,data_types;

var generator;

generator = 0;

function getGeneratedId(){
    return generator++;
}


function checkProgress() {
    $.ajax({
        url: "http://localhost:4567/progress",
        type: 'GET',
        // String result here
        success: function(res) {
            if(res.split(" - ")[0] == 'done') {
                alert('Best configuration: '+res.split(" - ")[2]);
                window.location = "http://localhost:4567/getresults";
            }else if(res == 'error')
                window.location = "http://localhost:4567/error";
            else {
                $('#progress').text(res + " %");
                $("#pbar").css('width',res + "%");
                $("#pbar").text(res + " %")
                setTimeout(checkProgress(), 500);
            }
        }
    });

}

function addAlgorithmsToControl(name){
    $("#algorithm_names").append($("<option>").text(name).val(name))
}


function initObjectiveControl(objdivid,obj_filename,iteration_count){
    $("#"+objdivid).append("<div id =\"objectiveiterationdiv\" name=\"objectiveiterationdiv\" ></div>\
                            <div id =\"objectivelistdiv\" name=\"objectivelistdiv\" ></div>\
                            <div id =\"objectivenewbuttondiv\" name=\"objectivenewbuttondiv\" ></div>\
                            <div id =\"objectivefilebrowserdiv\" name=\"objectivefilebrowserdiv\" ></div>"
    )

    var error = "Objectives are needed!";
    $("#objectivefilebrowserdiv").append("<input type=\"checkbox\" id =\"use_file_output\" name=\"use_file_output\">")
    $("#objectivefilebrowserdiv").append("<input type=\"file\" id =\"objFileBrowser\" name=\"objFileBrowser\" disabled>")
    $("#objectivefilebrowserdiv").append("<input type=\"text\" id =\"objFileName\" name=\"objFileName\" disabled>")
    if(obj_filename!==""){
        $('#objFileName').val(obj_filename)
    }
    else{
        $('#objFileName').get(0).setCustomValidity(error);
    }


    $("#objFileBrowser").change(function(){$("#objFileName").val($(this).val()) })
    $("#objFileName").change(function(){this.setCustomValidity(""); })
    $("#objectiveiterationdiv").append("<p>Number of iterations: </p>")
    $("#objectiveiterationdiv").append("<input id = \"use_iterations\" name =  \"use_iterations\" type=\"checkbox\" value=\"use_iterations\">")
    $("#objectiveiterationdiv").append("<input id = \"iterationCount\" type=\"text\" name=\"iterationCount\">")
    $("#objectivenewbuttondiv").append("<button type=\"button\" class=\"btn btn-default\" onclick = \"addNewObjective()\">Add objective</button>")

    if(iteration_count!=""){
        $('#iterationCount').val(iteration_count)
        $('#use_iterations').prop('checked', true);
    }
    $("#use_file_output").change(function(){
        var is_checked = $(this).is(':checked');
        $("#objFileName").prop('disabled', is_checked);
        $("#objFileBrowser").prop('disabled', is_checked);

    })



}
var objective_counter = 0
var param_counter = 0



var name_type_map = new Map();
var name_param_id_map = new Map();
var enum_value_map = new Map();

var kvArray = [["java.lang.Integer", "_int"], ["java.lang.Float", "_float"], ["java.lang.Boolean", "_bool"], ["Function", "_func"], ["Enum", "_string"], ["java.lang.Double", "_float"]];
var type_name_postfix_map = new Map(kvArray)

function updateType(name){


    for (var i in data_types) {
        $("."+name+type_name_postfix_map.get(data_types[i])).each(function(){$(this).hide();$(this).attr('disabled',true);})
    }
    var to_show_id = "."+name+type_name_postfix_map.get(ParamTypeFromName(name))

    $( to_show_id).each(function(){$(this).show();$(this).attr('disabled',false);})
    //if(ParamTypeFromName(name))
    //if(ParamTypeFromName(name))
    testEnum(name)


}
//we need for all the parameters for the case we want to use values in dependency handling before the param is added to control
function initMaps(paramname,param_type,enum_values){
    name_type_map.set(paramname,param_type)
    name_param_id_map.set(paramname,paramname)
    enum_value_map.set(paramname, enum_values.split(";"));

}
function updateAlgorithmParamVisibility(alg_name){

    $("#algorithm_names > option").each(function() {
        $("."+this.text).each(function(){$(this).hide();$(this).attr('disabled',true);})
    });


    $("."+alg_name).each(function(){$(this).show();$(this).attr('disabled',false);})


}

function getParamIdFromName(name){
    return name_param_id_map.get(name);
}

function printMap(map){
    var res = ""
    map.forEach(
        function(value, key){
            res += value +":"+key +","
        }
    )
    return res
}

function getParamNameFromId(id){
    var res
    name_param_id_map.forEach(
        function(value, key){
            if(id === value){
                res = key
            }
        }
    )
    return res

}

function getParamTypeFromId(pid){
    return name_type_map.get(pid);
}

function ParamTypeFromName(name){
    return name_type_map.get(name);
    //return name_type_map.get(getParamIdFromName(name));
}


function recursiveTypeTest(node, type_name, variable_name) {
    if (node.nodeType == 3) { // text node
        node.nodeValue = node.nodeValue.replace("1", "۱");
    } else if (node.nodeType == 1) { // element
        $(node).contents().each(function () {
            recursiveReplace(this);
        });
    }
}


// just because cool
function recursiveReplace(node) {
    if (node.nodeType == 3) { // text node
        node.nodeValue = node.nodeValue.replace("1", "۱");
    } else if (node.nodeType == 1) { // element
        $(node).contents().each(function () {
            recursiveReplace(this);
        });
    }
}
//addNewObjective("$obj.getName()","$obj.getRelation()","$obj.getTarget()","$obj.isTerminator()","$obj.getTypeName()")

function addNewObjective(name,relation,target,terminator,typename ){

    if(name === undefined){
        while($("#object_names").val().includes("obj_"+objective_counter) ){objective_counter++}
        name = "obj_"+objective_counter //$("#objectivelistdiv").children().length
    }
    var id = name+"_objective_div"
    $("#object_names").val($("#object_names").val()+";"+id)
    $("#objectivelistdiv").append("<div id =\""+id+ "\" name=\""+ id+ "\" ></div>")
    $("#"+id).append("Objective: ")
    $("#"+id).append("<input id = \""+id+"_name\" name = \""+id+"_name\" type=\"text\" value = \""+name+"\">")

    $("#"+id).append("Weight: ")

    $("#"+id).append("<input id = \""+id+"_weight\" name = \""+id+"_weight\" type=\"text\" value = \"100\">")

    $("#"+id).append("Relation: ")
    var sel1 = $("<select id = \""+id+"_relation\" name = \""+id+"_relation\">").appendTo($("#"+id));




    /*  if(relation == "GREATER_THAN" || relation == "LESS_THAN" || relation == "EQUALS" ){
          $("#"+id+"_relation_value").show();
          $("#"+id+"_relation_value").attr('disabled', false);

      }else{
          $("#"+id+"_relation_value").hide();
          $("#"+id+"_relation_value").attr('disabled', true);
      }*/

    target = target==undefined?0:target;
    $("#"+id).append("<input id = \""+id+"_relation_value\" name = \""+id+"_relation_value\" type=\"number\" step=\"any\" value = \""+target+"\">")
    //$("#"+id).append("Terminates optimization: ")
    //$("#"+id).append("<input id = \""+id+"_terminator\" name = \""+id+"_terminator\" type=\"checkbox\" value = \""+terminator+"\">")
    $("#"+id).append("Type: ")
    var sel = $("<select id = \""+id+"_type\" name = \""+id+"_type\">").appendTo($("#"+id));
    $(objective_types).each(function(t,e) {
        sel.append($("<option>").text(e));
    });
    $("#"+id+"_type option").filter(function() {
        //may want to use $.trim in here
        return $(this).text() == typename;
    }).prop('selected', true);

    $("#"+id).append("<button id = \""+id+"_remove_btn\" name = \""+id+"_remove_btn\" type=\"button\" onclick=\"removeObjective('"+id+"')\">Remove</button>")

    $("#"+id+"_relation").on('change', function (e) {
        var optionSelected = $("option:selected", this);
        var valueSelected = this.value;
        if(valueSelected == "GREATER_THAN" || valueSelected == "LESS_THAN" || valueSelected == "EQUALS" || valueSelected == "MAXIMIZE_TO_CONVERGENCE" || valueSelected == "MINIMIZE_TO_CONVERGENCE"){
            $("#"+id+"_relation_value").show();
            $("#"+id+"_relation_value").attr('disabled', false);

        }else{
            $("#"+id+"_relation_value").hide();
            $("#"+id+"_relation_value").attr('disabled', true);
        }

    });

    $(objective_relation_types).each(function(t,e) {
        sel1.append($("<option>").text(e));
    });
    $("#"+id+"_relation option").filter(function() {
        //may want to use $.trim in here
        return $(this).text() == relation;
    }).prop('selected', true);

    if(relation == "GREATER_THAN" || relation == "LESS_THAN" || relation == "EQUALS" || relation == "MAXIMIZE_TO_CONVERGENCE" || relation == "MINIMIZE_TO_CONVERGENCE"){
        $("#"+id+"_relation_value").show();
        $("#"+id+"_relation_value").attr('disabled', false);

    }else{
        $("#"+id+"_relation_value").hide();
        $("#"+id+"_relation_value").attr('disabled', true);
    }



}

function removeObjective(id){
    $("#object_names").val($("#object_names").val().replace(id,""))
    $("#"+id).remove()
}
function addNewParam(parent_id){
    var name = "param" + param_counter//++
    var paramdivid = name + "_div"
    param_names.push(name)
    $(".paramselector" ).each(
        function() {$(this).append($("<option></option>")
            .attr("value",name)
            .text(name)
        )}
    )

    addParamDiv(parent_id,paramdivid,name,data_types[0],"");
    addEmptyDependency(paramdivid,name, "0","0","0","");

    updateType(name);

}
function addEmptyDependency(paramdivid, paramname, value, lower, upper, other,modifiable = true){
    var dependencyid = paramdivid+"_paramdiv_dep_"+paramname+getGeneratedId();
    $("#param_range_div_ids").val($("#param_range_div_ids").val()+";"+dependencyid)
    //$("#dependency_ids").value += ";"+  dependencyid
    addEmptyDependencyControl(paramdivid, paramname, dependencyid,value,lower, upper, other,modifiable)
    if(modifiable){
        //addAddDependencyButton(paramdivid,paramname,dependencyid)
        //addRemoveRangeButton(paramdivid,paramname,dependencyid)
        addDependencyDiv(paramdivid, paramname,dependencyid)
        hideRemoveDependencyButton(paramdivid, paramname,dependencyid)
        var new_reangebutton = document.getElementById(paramdivid+"add_new_range_btn")
        if(!!new_reangebutton){
            document.getElementById(paramdivid).removeChild(new_reangebutton)
        }
    }
    updateType(paramname)
}

var commandlinetextinputid = "commandinput"

function addCommandLine(commandline){

    $("#commanddiv").append("<textarea  id=\""+commandlinetextinputid+"\" name=\"commandinput\" cols = \" 100 \" value= \""+commandline+"\">")
    $("#"+commandlinetextinputid).val(commandline);

    //$("#commanddiv").append("<input type = \"text\" id=\""+commandlinetextinputid+"\" name=\"commandinput\" size = \" 100 \"value= \""+commandline+"\">")
    $("#"+commandlinetextinputid).on('change',function(){
            checkCommandLine()
        }
    )
}

function checkCommandLine()
{
    var cltext = $("#"+commandlinetextinputid).val().split(" ")
    for(var w in cltext){
        var word = cltext[w]
        if(word.charAt(0) == '$' && !name_param_id_map.has(word.substr(1)) ){
            $("#"+commandlinetextinputid).get(0).setCustomValidity("Undefined parameter: "+ word)
            return;
        }
    }
    $("#"+commandlinetextinputid).get(0).setCustomValidity("");

}
//               // <p class='"+paramname+"_string' class='"+paramname+"_float' class='"+paramname+"_int' > \
//<p/>\
function addEmptyDependencyControl(paramdivid,paramname,depenedencyid, value,lower, upper, otherInfo, modifiable = true){
    $("#"+paramdivid).append("<div id = \"range_"+depenedencyid+"\" class = 'range'>\
                <input type=\"text\" name=\""+depenedencyid+"_value_string\" id = \""+depenedencyid+"_value_string\"  value =\""+value+"\" class='"+paramname+"_string "+paramname+"_input_string' required>\
                <input type=\"text\" placeholder = \"Give the formula here! \"text=\"formula\" name=\""+depenedencyid+"_value_string_func\" id = \""+depenedencyid+"_value_string_func\"  value =\""+otherInfo+"\" class='"+paramname+"_func' required>\
                <span class='"+paramname+"_func'>Number of generated values :</span>\
                <input type=\"number\" name=\""+depenedencyid+"_value_int_func\" id = \""+depenedencyid+"_value_int_func\"  value =\""+10+"\" class='"+paramname+"_func'required >\
                <input type=\"number\" name=\""+depenedencyid+"_value_int\" id = \""+depenedencyid+"_value_int\"  value =\""+value+"\" class='"+paramname+"_int' required>\
                <input type=\"number\" step=\"any\" name=\""+depenedencyid+"_value_float\" id = \""+depenedencyid+"_value_float\"  value =\""+value+"\" class='"+paramname+"_float' required>\
                <select  name=\""+depenedencyid+"_value_bool\" id = \""+depenedencyid+"_value_bool\"  value =\""+value+"\" class='"+paramname+"_bool' required ></select>\
                <span class = '"+paramname+"_string "+paramname+"_float "+paramname+"_int'>Range :</span><span class = '"+paramname+"_string "+paramname+"_float "+paramname+"_int'> From: </span><input type=\"text\" name=\""+depenedencyid+"_lower_string\" id = \""+depenedencyid+"_lower_string\"  value =\""+lower+"\" class='"+paramname+"_string "+paramname+"_input_string' required>\
                <input type=\"number\" name=\""+depenedencyid+"_lower_int\" id = \""+depenedencyid+"_lower_int\"  value =\""+lower+"\" class='"+paramname+"_int' required>\
                                <input type=\"number\" step=\"any\" name=\""+depenedencyid+"_lower_float\" id = \""+depenedencyid+"_lower_float\"  value =\""+lower+"\" class='"+paramname+"_float' required>\
                <span class = '"+paramname+"_string "+paramname+"_float "+paramname+"_int'>To: </span><input type=\"text\" name=\""+depenedencyid+"_upper_string\" id = \""+depenedencyid+"_upper_string\"  value =\""+upper+"\" class='"+paramname+"_string "+paramname+"_input_string' required> \
                <input type=\"number\" name=\""+depenedencyid+"_upper_int\" id = \""+depenedencyid+"_upper_int\"  value =\""+upper+"\" class='"+paramname+"_int'> \
                <input type=\"number\" step=\"any\" name=\""+depenedencyid+"_upper_float\" id = \""+depenedencyid+"_upper_float\"  value =\""+upper+"\" class='"+paramname+"_float' required> \
                </div>")


    //boolean_selector.setAttribute("id", rangeid+"_dep_lower_bool");
    //boolean_selector.setAttribute("name", rangeid+"_dep_lower_bool");
    //boolean_selector.setAttribute("class", "selected_param+"_bool"");
    //for (var i = 0; i < paramarray.length; i++) {
    var bool_array = [true,false]
    for (var i = 0; i < bool_array.length; i++) {
        //var p =param_names[i];

        var opt = document.createElement("option");
        opt.setAttribute("value",bool_array[i]);
        //opt.setAttribute("name", rangeid+"_select_other_name_"+ name_param_id_map.get(p));
        var name = document.createTextNode(bool_array[i]);
        opt.appendChild(name)
        $('#'+depenedencyid+"_value_bool").append(opt)
    }
    if( !jQuery.inArray($('#'+depenedencyid+"_value_bool").val(),bool_array))
    {
        $('#'+depenedencyid+"_value_bool").val("true")
    }


    if(!modifiable){
        $("#"+depenedencyid+"_upper_float").prop("readonly", true);
        $("#"+depenedencyid+"_lower_float").prop("readonly", true);
        $("#"+depenedencyid+"_upper_int").prop("readonly", true);
        $("#"+depenedencyid+"_lower_int").prop("readonly", true);
        $("#"+depenedencyid+"_upper_string").prop("readonly", true);
        $("#"+depenedencyid+"_lower_string").prop("readonly", true);
        $("#"+depenedencyid+"_lower_string").prop("readonly", true);
    }
    $("."+paramname+"_value_string").on('change',function () {testEnum(paramname)})
    $("."+paramname+"_lower_string").on('change',function () {testEnum(paramname)})
    $("."+paramname+"_upper_string").on('change',function () {testEnum(paramname)})
    $("."+paramname+"_string").on('change',function () {testEnum(paramname)})

}
function addParamWithDependency(paramdivid, paramname,value,lower, upper, othername,otherlower, otherupper, other, modifiable = true){
    var dependencyid = paramdivid+ "_paramdiv_dep_"+paramname+getGeneratedId();
    $("#param_range_div_ids").val($("#param_range_div_ids").val()+";"+dependencyid)

    addEmptyDependencyControl(paramdivid,paramname,dependencyid,value,lower, upper,other, modifiable)
    if(modifiable){
        addDependencyDiv(paramdivid,paramname,dependencyid)
        // addAddDependencyButton(paramdivid,paramname,dependencyid)
        // addRemoveRangeButton(paramdivid,paramname,dependencyid)
    }
    addDependency(paramdivid, paramname, dependencyid,otherlower,otherupper,othername, modifiable)

}

function addDependencyDiv(paramdivid,paramname,dependencyid){
    $("#range_"+dependencyid).append("<div id ='range_"+dependencyid+  "_div' />")
    addAddDependencyButton(paramdivid,paramname,dependencyid);
    addRemoveRangeButton(paramdivid,paramname,dependencyid);
    addRemoveDependencyButton(paramdivid,paramname,dependencyid);

}



function addAddDependencyButton(paramdivid,paramname, dependencyid){
    $("#range_"+dependencyid+"_div").append("<button id = \""+dependencyid+"_addDependencyButton\" type=\"button\" onclick=\"addDependency(\'"+paramdivid+"\',\'"+paramname+"\',\'"+dependencyid+"\','','','')\">New param dependency</button>")
    //$("#range_"+depenedencyid+).append("<button id = \""+paramdivid+"_addDependencyButton\" type=\"button\" onclick=\"addDependency(\'"+paramdivid+"\',\'"+paramname+"\',\'"+depenedencyid+"\','','')\">New param dependency</button>")
}
//??????
function showAddDependencyButton(paramdivid,paramname, depenedencyid){
    var id = "#"+ depenedencyid+"_addDependencyButton";
    $(id).show()
    $(id).attr('disabled', false);
}

function hideAddDependencyButton(paramdivid,paramname, depenedencyid){
    var id = "#"+ depenedencyid+"_addDependencyButton";
    $(id).hide()
    $(id).attr('disabled', true);
}


function addRemoveDependencyButton(paramdivid,paramname, dependencyid){
    var rangeid = "range_"+dependencyid;


    var btn = document.createElement("BUTTON");
    btn.setAttribute("id", rangeid+"_remove_dep_btn");
    btn.setAttribute("type","button");

    btn.setAttribute("onclick", "removeDependency(\'"+paramdivid+"\', \'"+paramname+"\', \'"+dependencyid +"\')");
    btn.innerHTML+= "Remove dependency"
    $("#range_"+dependencyid+"_div").append(btn)

}
//these are not needed they are supposed to belong to a div to be destroyed
/*function showRemoveRangeButton(paramdivid,paramname, depenedencyid){
    var id = "#"+paramdivid+"_removeRangeButton" ;
    $(id).attr('disabled', false);
}

function hideRemoveRangeButton(paramdivid,paramname, depenedencyid){
    var id = "#"+paramdivid+"_removeRangeButton" ;
    $(id).attr('disabled', true);
}*/

function showNewRangeButton(paramdivid,paramname, depenedencyid){
    var id = "#"+paramdivid+"add_new_range_btn";
    $(id).attr('disabled', false);
}

function hideNewRangeButton(paramdivid,paramname, depenedencyid){
    var id = "#"+paramdivid+"add_new_range_btn";
    $(id).hide()
    $(id).attr('disabled', true);
}

function addRemoveRangeButton(paramdivid,paramname, depenedencyid){
    $("#range_"+depenedencyid+"_div").append("<button id = \""+depenedencyid+"_removeRangeButton\" type=\"button\" onclick=\"removeRange(\'"+paramdivid+"\',\'"+paramname+"\',\'"+depenedencyid+"\')\">Remove range</button>")
}

function removeRange(paramdivid, paramname, depenedencyid){
    var rangeid = "range_"+depenedencyid
    $("#param_range_div_ids").val( $("#param_range_div_ids").val().replace(depenedencyid,""))
    var rangeDivToRemove = document.getElementById(rangeid)
    rangeDivToRemove.parentNode.removeChild(rangeDivToRemove)
    var range_count = $('#paramdivid').find('.range').length;
    var dep_count = $('#paramdivid').find('.dep').length;
    //that is that all range has a dependency so thus all of them are bounded
    if(range_count==dep_count)
        placeNewRangeButton(paramdivid,paramname)
   /* if($("#"+paramdivid).children('range').length == 0){
        placeNewRangeButton(paramdivid,paramname)
    }*/
}


function addDependency(paramdivid, paramname, depenedencyid,deplower,depupper,othername, modifiable = true){
    var rangeid = "range_"+depenedencyid
    $("#param_names").val($("#param_names").val()+";"+rangeid)


    var selector = document.createElement("SELECT");
    selector.setAttribute("id", rangeid+"_select_other_name");
    selector.setAttribute("name", rangeid+"_select_other_name");
    selector.setAttribute("class", "paramselector");
    //for (var i = 0; i < paramarray.length; i++) {
    for (var i = 0; i < param_names.length; i++) {
        var p =param_names[i];
        if(p.localeCompare(paramname)!==0){
            console.log("element "+ p)
            var opt = document.createElement("option");
            opt.setAttribute("value", name_param_id_map.get(p));
            opt.setAttribute("name", rangeid+"_select_other_name_"+ name_param_id_map.get(p));
            var name = document.createTextNode(p);
            opt.appendChild(name);
            selector.appendChild(opt);
        }

    }
    if(othername != '') {
        dep_value = name_param_id_map.get(othername);
        $(selector).val(dep_value);
    }
    //$( "#selector option:selected" ).text();

    var tn1 =  document.createTextNode(", if param ");
    //document.getElementById(rangeid).appendChild(document.createTextNode(", if param "))
    //document.getElementById(rangeid).appendChild(selector)
    var tn2 = document.createTextNode(" in [");
    //document.getElementById(rangeid).appendChild(document.createTextNode(" in "))
    //


    //var selected_param =  $(selector).val();
    var selected_param =  getParamNameFromId($(selector).val());

    var input1_string = document.createElement("input");
    var input1_int = document.createElement("input");
    var input1_float = document.createElement("input");
    var input1_bool = document.createElement("input");

    input1_string.type = "text";
    input1_string.setAttribute("id", rangeid+"_dep_lower_string");
    input1_string.setAttribute("name", rangeid+"_dep_lower_string");
    input1_string.setAttribute("value", deplower);
    input1_string.setAttribute("class", selected_param+"_input_string "+ selected_param+"_string");
    input1_string.setAttribute("required", true);


    input1_int.type = "number";
    input1_int.setAttribute("id", rangeid+"_dep_lower_int");
    input1_int.setAttribute("name", rangeid+"_dep_lower_int");
    input1_int.setAttribute("value", deplower);
    input1_int.setAttribute("class", selected_param+"_int");
    input1_int.setAttribute("required", true);


    input1_float.type = "number";
    input1_float.setAttribute("step", "any");
    input1_float.setAttribute("id", rangeid+"_dep_lower_float");
    input1_float.setAttribute("name", rangeid+"_dep_lower_float");
    input1_float.setAttribute("value", deplower);
    input1_float.setAttribute("class", selected_param+"_float");
    input1_float.setAttribute("required", true);

    /*input1_bool.type = "checkbox";
    input1_bool.setAttribute("id", rangeid+"_dep_lower_bool");
    input1_bool.setAttribute("name", rangeid+"_dep_lower_bool");
    input1_bool.setAttribute("class", selected_param+"_bool");
    input1_bool.setAttribute("value", deplower);
    input1_bool.setAttribute("required", true);input1_bool.type = "checkbox";*/

    var boolean_selector = document.createElement("SELECT");
    boolean_selector.setAttribute("id", rangeid+"_dep_lower_bool");
    boolean_selector.setAttribute("name", rangeid+"_dep_lower_bool");
    boolean_selector.setAttribute("class", selected_param+"_bool");
    //for (var i = 0; i < paramarray.length; i++) {
    var bool_array = [true,false]
    for (var i = 0; i < bool_array.length; i++) {
        var opt = document.createElement("option");
        opt.setAttribute("value",bool_array[i]);

        var name = document.createTextNode(bool_array[i]);
        opt.appendChild(name)
        boolean_selector.appendChild(opt)

    }
    if( !jQuery.inArray($('#'+rangeid+"_dep_lower_bool").val(),bool_array))
    {
        $('#'+rangeid+"_dep_lower_bool").val("true")
    }

    /*input1_bool.setAttribute("id", rangeid+"_dep_lower_bool");
    input1_bool.setAttribute("name", rangeid+"_dep_lower_bool");
    input1_bool.setAttribute("class", selected_param+"_bool");
    input1_bool.setAttribute("value", deplower);
    input1_bool.setAttribute("required", true);*/

    var tn3 = document.createTextNode(", ")
    var input2 = document.createElement("input");

    var input2_string = document.createElement("input");
    var input2_int = document.createElement("input");
    var input2_float = document.createElement("input");
    var input2_bool = document.createElement("input");

    input2_string.type = "text";
    input2_string.setAttribute("id", rangeid+"_dep_upper_string");
    input2_string.setAttribute("name", rangeid+"_dep_upper_string");
    input2_string.setAttribute("value", depupper);
    input2_string.setAttribute("class", selected_param+"_string");
    input2_string.setAttribute("class", selected_param+"_input_string "+ selected_param+"_string");
    input2_string.setAttribute("required", true);

    input2_int.type = "number";
    input2_int.setAttribute("id", rangeid+"_dep_upper_int");
    input2_int.setAttribute("name", rangeid+"_dep_upper_int");
    input2_int.setAttribute("value", depupper);
    input2_int.setAttribute("class", selected_param+"_int");
    input2_int.setAttribute("required", true);



    input2_float.type = "number";
    input2_float.setAttribute("step", "any");
    input2_float.setAttribute("id", rangeid+"_dep_upper_float");
    input2_float.setAttribute("value", depupper);
    input2_float.setAttribute("name", rangeid+"_dep_upper_float");
    input2_float.setAttribute("class", selected_param+"_float"); ///todo ezt kellene valószínű
    input2_float.setAttribute("required", true);

    input2.type = "text";
    input2.setAttribute("id", rangeid+"_dep_upper");
    input2.setAttribute("name", rangeid+"_dep_upper");
    input2.setAttribute("value", depupper);
    input2.setAttribute("required", true);
    var tn4 =  document.createTextNode("]")

    /*var btn = document.createElement("BUTTON");
    btn.setAttribute("id", rangeid+"_remove_dep_btn");
    btn.setAttribute("onclick", "removeDependency(\'"+paramdivid+"\', \'"+paramname+"\', \'"+depenedencyid +"\')");
    btn.innerHTML+= "Remove dependency"*/


    // var element = document.getElementById(paramdivid+"_addDependencyButton");
    // element.parentNode.removeChild(element);
    hideAddDependencyButton(paramdivid,paramname, depenedencyid)
    showRemoveDependencyButton(paramdivid,paramname, depenedencyid)
    if(!modifiable){
        $("#"+rangeid+"_dep_upper_float").prop("readonly", true);
        $("#"+rangeid+"_dep_lower_float").prop("readonly", true);
        $("#"+rangeid+"_dep_upper_int").prop("readonly", true);
        $("#"+rangeid+"_dep_lower_int").prop("readonly", true);
        $("#"+rangeid+"_dep_upper_string").prop("readonly", true);
        $("#"+rangeid+"_dep_lower_string").prop("readonly", true);
        $("#"+rangeid+"_dep_lower_string").prop("readonly", true);
    }
    else{

        var divid = rangeid+ "_other_param_div"

        //$("<div id='"+ rangeid+ "_other_param_div'></div>").insertBefore('#'+depenedencyid+"_removeRangeButton");
        $("<div class='dep' id='"+ rangeid+ "_other_param_div'></div>").insertBefore('#range_'+depenedencyid+  "_div");
        document.getElementById(divid).appendChild(tn1)
        document.getElementById(divid).appendChild(selector)
        document.getElementById(divid).appendChild(tn2)
        document.getElementById(divid).appendChild(input1_string)
        document.getElementById(divid).appendChild(input1_int)
        document.getElementById(divid).appendChild(input1_float)
        document.getElementById(divid).appendChild(boolean_selector)

        document.getElementById(divid).appendChild(tn3)
        document.getElementById(divid).appendChild(input2_string)
        document.getElementById(divid).appendChild(input2_int)
        document.getElementById(divid).appendChild(input2_float)
        document.getElementById(divid).appendChild(tn4)
        //document.getElementById(divid).appendChild(btn)
    }
    $("#"+rangeid+"_dep_lower_string").on('change', function (e) {
        testEnum(selected_param)}
    );
    $("#"+rangeid+"_dep_upper_string").on('change', function (e) {
        testEnum(selected_param)}
    );
    if($("#"+paramdivid).find('.range').length  == $("#"+paramdivid).find('.dep').length){
        placeNewRangeButton(paramdivid,paramname)
    }


    (function () {
        var previous;
        $(selector).on('focus', function () {
            // Store the current value on focus and on change
            previous = this.value;
        })
            .on('change', function (e) {


                var optionSelected = $("option:selected", this);
                var valueSelected = this.value;
                var name = getParamNameFromId(valueSelected);

                //reset all the dependency input class to new paramname
                var x = $("#"+rangeid+ "_other_param_div > ."+previous+"_int")
                $("#"+rangeid+ "_other_param_div > ."+previous+"_int").addClass(name+"_int").removeClass(previous+"_int")
                x = $("#"+rangeid+ "_other_param_div > ."+previous+"_int")
               // $("#"+rangeid+ "_other_param_div > ."+previous+"_int").removeClass(previous+"_int")
                $("#"+rangeid+ "_other_param_div > ."+previous+"_float").addClass(name+"_float").removeClass(previous+"_float")
               // $("#"+rangeid+ "_other_param_div > ."+previous+"_float").removeClass(previous+"_float")
                $("#"+rangeid+ "_other_param_div > ."+previous+"_bool").addClass(name+"_bool").removeClass(previous+"_bool")
                //$("#"+rangeid+ "_other_param_div > ."+previous+"_bool").removeClass(previous+"_bool")
                $("#"+rangeid+ "_other_param_div > ."+previous+"_string").addClass(name+"_string").removeClass(previous+"_string")
                //$("#"+rangeid+ "_other_param_div > ."+previous+"_string").removeClass(previous+"_string")


                var new_type = name_type_map.get(name_param_id_map.get(valueSelected))
               // renameDependencyClasses()
                updateType(name)


            })})();
    updateType($( "#"+rangeid+"_select_other_name option:selected" ).text());

}

function  showRemoveDependencyButton(paramdivid,paramname, depenedencyid){
    var rangeid = "range_"+depenedencyid;
    var rdbid =  rangeid+"_remove_dep_btn";
    $('#'+rdbid).show()
    $('#'+rdbid).attr('disabled', false);

}

function  hideRemoveDependencyButton(paramdivid,paramname, depenedencyid){
    var rangeid = "range_"+depenedencyid;
    var rdbid =  rangeid+"_remove_dep_btn";
    $('#'+rdbid).hide()
    $('#'+rdbid).attr('disabled', true);

}

function placeNewRangeButton(paramdivid, paramname){


    var new_reangebutton = document.getElementById(paramdivid+"add_new_range_btn")
    if(!!new_reangebutton){
        document.getElementById(paramdivid).removeChild(new_reangebutton)
    }else{
        new_reangebutton = document.createElement("BUTTON");
        new_reangebutton.setAttribute("id", paramdivid+"add_new_range_btn");
        new_reangebutton.setAttribute("type","button");

        new_reangebutton.setAttribute("onclick", "addEmptyDependency(\'"+paramdivid+"\', \'"+paramname+"\', \' 0 ',\' 0 \',\' 0 \')");
        new_reangebutton.innerHTML+= "New range"
    }
    document.getElementById(paramdivid).appendChild(new_reangebutton)

}

function hideNewRangeButton(paramdivid){
    var id ="#"+ paramdivid+"add_new_range_btn";
    $(id).attr('disabled',true);
}

function removeNewRangeButton(paramdivid){
    var new_reangebutton = document.getElementById(paramdivid+"add_new_range_btn")
    if(!!new_reangebutton){
        document.getElementById(paramdivid).removeChild(new_reangebutton)
    }
}

function addSomething(name){
    var id = "#"+name
    $(id).append("<p>Text.</p>")

}
function addParamDiv(parentNode,paramdivid, paramname,param_type, enum_values, modifiable = true){
    param_counter++

    $("#dependency_ids").val($("#dependency_ids").val()+";"+paramdivid)
    $("#"+parentNode).append("\
    <div id = \""+paramdivid+"\" name=\""+paramdivid+"\" class = \"paramcontainer\">\
    <p><b>Parameter Name: </b><input class = \"paramname\" type = \"text\" id =\""+paramdivid+"paramname\" name=\""+paramdivid+"paramname\" value= \"" +paramname+"\"> </p>\
    </div>"
    )
    // from inputnamechange = \"paramNameEdited(\""+paramdivid+"paramname\")\"

    $("#"+paramdivid).append("Type: ")
    var sel = $("<select id = \""+paramdivid+"_type\" name = \""+paramdivid+"_type\">").appendTo($("#"+paramdivid));
    $(data_types).each(function(t,e) {
        sel.append($("<option>").text(e));
    });

    if(param_type === "java.lang.Double"){
        param_type = "java.lang.Float"
    }
    $("#"+paramdivid+"_type option:contains(" + param_type + ")").attr('selected', 'selected');
    if(!modifiable){
        $("#"+paramdivid+"paramname").prop("readonly", true);
        //$("#"+paramdivid+"_type").prop("readonly", true);
        $("#"+paramdivid+"_type").attr("disabled", true);
    }

    // "."+name+type_name_postfix_map.get(data_types[i])).each(function(){$(this).hide()
    $("<div id = \""+paramdivid+"_enum_descriptor_div\"><span class = \""+paramname+"_string\">Values : </span><input class = \""+paramname+"_string\" type = \"text\" id =\""+paramdivid+"_enum_descriptor\" name=\""+paramdivid+"_enum_descriptor\" value= \"" +enum_values+"\" > </div>").appendTo($("#"+paramdivid));

    //itt id vagy name alapján kerülnek be a dolgok???
    enum_value_map.set(paramname, enum_values.split(";"));
    $("#"+paramdivid+"_type").on('change', function (e) {
        var optionSelected = $("option:selected", this);
        var valueSelected = this.value;
        var n = getParamNameFromId(paramname)
        name_type_map.set(n,valueSelected)
        updateType(n)

        var found = $( "."+paramname )
        found.each(function(){$(this).css("background-color", 'green')})
        var found_wrong = found.filter(function(){
            return jQuery.type(this.value)!==mapJSTypeFrom(valueSelected)
        })
        found_wrong.each(function(){$(this).css("background-color", 'red')})

    });

    $("#"+paramdivid+"paramname").on('change', function (e) {
        var new_name = $( this ).val()
        //var old_name = getParamNameFromId(paramname)
        //paramNameEdited(new_name, old_name)

        var id_of_param = paramname;// we use first value as id
        paramNameEdited(new_name, id_of_param)
    });
    $("#"+paramdivid+"_enum_descriptor").on('focusout', function () {
        var content = $(this).val();
        enum_value_map.set(paramname,content.split(";"))
        testEnum(paramname)

    });
    name_type_map.set(paramname,param_type)
    name_param_id_map.set(paramname,paramname)
    $("#name_param_id_map").val(printMap(name_param_id_map))
    if(modifiable){
        $("#"+parentNode).append("<button type=\"button\" id = '"+paramdivid+"_remove_btn' onclick='deleteParam(\""+parentNode+"\",\""+paramdivid+"\",\""+paramdivid+"_remove_btn\",\""+paramname+"\")'>Delete Parameter</button>");
    }
    $("#"+parentNode).append("<hr id='"+paramdivid+"endline' />");

}

function deleteParam(parentNode, paramdivid, remove_btn_id,paramname){
    $("#dependency_ids").val($("#dependency_ids").val().replace(paramdivid,''));
    var x = $("#param_range_div_ids").val();
    for(var z  of x.split(";"))
        if(z.includes(paramdivid))
            $("#param_range_div_ids").val($("#param_range_div_ids").val().replace(z,""))

    name_type_map.delete(getParamNameFromId(paramname));
    name_param_id_map.delete(getParamNameFromId(paramname))
    //$("#"+parentNode).remove("#"+paramdivid);
    //$("#"+parentNode).remove("#"+remove_btn_id);
    $("#"+paramdivid).remove();
    $("#"+remove_btn_id).remove();
    $("#"+paramdivid+"endline").remove();

    checkCommandLine();
}
function testEnum(paramname){
    $("."+getParamNameFromId(paramname)+"_input_string").each(function(){
        if( enum_value_map.get(paramname)==='udefined' ||enum_value_map.get(paramname).indexOf($(this).val())===-1){
            var error = "Not in range!";
            this.setCustomValidity(error);
        }
        else{
            this.setCustomValidity("");
        }

    })
}

function mapJSTypeFrom(typeNameFromEnum){
    var type = null
    if(typeNameFromEnum === "Boolean")
        type="boolean"
    else if (typeNameFromEnum === "Float" || valueSelected === "Integer")
        type="number"
    else if(typeNameFromEnum === "Enum" )
        type ="string"
    return type

}
function updateParamNames(name, oldname){
    var index = param_names.indexOf(oldname);

    if (index !== -1) {
        param_names[index] = name;
    }

    var id = name_param_id_map.get(oldname)
    name_param_id_map.set(name,id)
    name_param_id_map.delete(oldname);


    var type = name_type_map.get(oldname)
    name_type_map.set(name,type)
    name_type_map.delete(oldname);


}
function paramNameEdited(name, id){


    var oldname = getParamNameFromId(id);
    //var actualnewname =
    if(name == ""){
        name = id; // for the case someone deleted the name first
    }
    $( ".paramselector option" ).each(function(){

            if($(this).val() == id)
            {
                $(this).text(name)
                //val should be the id never to change
                //$(this).val(name)
            }

        }
    )


    //var id = name_param_id_map.get(oldname)
    name_param_id_map.set(name,id)
    name_param_id_map.delete(oldname);
    $("#name_param_id_map").val(printMap(name_param_id_map))
    var type = name_type_map.get(oldname)
    name_type_map.set(name,type)
    name_type_map.delete(oldname);


    $("."+oldname+"_int").each(function(){$(this).attr('class',$(this).attr('class').replace(oldname+"_int",name+'_int'))});
    $("."+oldname+"_float").each(function(){$(this).attr('class', $(this).attr('class').replace(oldname+"_float",name+'_float'))});
    $("."+oldname+"_bool").each(function(){$(this).attr('class', $(this).attr('class').replace(oldname+"_bool",name+'_bool'))});
    $("."+oldname+"_string").each(function(){$(this).attr('class', $(this).attr('class').replace(oldname+"_string",name+'_string'))});
    $("."+oldname+"_input_string").each(function(){$(this).attr('class',$(this).attr('class').replace(oldname+"_input_string",name+'_input_string'))});
    $("."+oldname+"_func").each(function(){$(this).attr('class', $(this).attr('class').replace(oldname+"_func",name+'_func'))});


    var index = param_names.indexOf(oldname);

    if (index !== -1) {
        param_names[index] = name;
    }
    updateCommandLine(name, oldname)
    checkCommandLine()
}

function updateCommandLine(name, oldname){
    var actual_text = $("#"+commandlinetextinputid).val()
    var splitted_text = actual_text.split(" ")
    var res = []
    for(var str of splitted_text){
        if(str == "$"+oldname){
            res.push("$"+name)
        }
        else{
            res.push(str)
        }
    }

    $("#"+commandlinetextinputid).val(res.join(" "))
}

function enableTypeSelectsAndSubmit(){
    var dependency_id_array = $("#dependency_ids").val().split(";");
    for(var ind in dependency_id_array){
        var id = dependency_id_array[ind];

        if(id!==""){
            $("#"+id+"_type").attr('disabled',false);
        }
    }
    $( "#paramform" ).attr('action','/run');
    $( "#paramform" ).submit();
    //window.location="/run";
}


function removeDependency(paramdivid, paramname, depenedencyid){
    var x = $("#dependency_ids").val();
    var y = $("#param_names").val();

    $("#param_names").val(y.replace('range_'+depenedencyid,'')); //todo rename this!!!!
    var element = document.getElementById("range_"+depenedencyid+ "_other_param_div");
    element.parentNode.removeChild(element);
    showAddDependencyButton(paramdivid,paramname, depenedencyid)
    hideRemoveDependencyButton(paramdivid,paramname, depenedencyid)
    removeNewRangeButton(paramdivid)


}
function recursiveTypeTest(node, typename) {
    if (node.nodeType == 3) { // text node
        node.nodeValue = node.nodeValue.replace("1", "۱");
    } else if (node.nodeType == 1) { // element
        $(node).contents().each(function () {
            recursiveReplace(this);
        });
    }
}


// not used
function recursiveReplace(node) {
    if (node.nodeType == 3) { // text node
        // if(node.nodeValue)
        node.nodeValue = node.nodeValue.replace("1", "۱");
    } else if (node.nodeType == 1) { // element
        $(node).contents().each(function () {
            recursiveReplace(this);
        });
    }
}


