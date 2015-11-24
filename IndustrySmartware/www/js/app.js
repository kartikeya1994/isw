$(document).ready(function() { 
	var statusList = {
			"25" : "IDLE",
			"30" : "UNDERGOING CORRECTIVE MAINTENANCE",
			"29" : "UNDERGOING PREVENTIVE MAINTENANCE",
			"26" : "EXECUTING JOB",
			"27" : "WAITING FOR LABOUR",
			"28" : "WAITING FOR LABOUR",
			"33" : "PLANNING",
			"39" : "REPLANNING",
			"38" : "PROCESS COMPLETE"
	};
	var machines = {};
	$("#connected-label").hide();
	var ws = new WebSocket("ws://"+window.location.hostname+":9091/");
	ws.onopen = function()
	{
		$("#disconnected-label").hide();
		$("#connected-label").show();
		
	};
		
	ws.onmessage = function (evt) 
	{ 
		var message = JSON.parse(evt.data);
		console.log(message);
		if (message.type == "machine")
			machines[message.ip] = new Machine(message.ip);
	};
		
	ws.onclose = function()
	{ 
		$("#disconnected-label").show();
		$("#connected-label").hide();
	};
	ws.onerror = function()
	{
		console.log("error");
	};
	
	function Machine(ip){
		var context = this;
		console.log("Adding machine: "+ ip);
		this.list_el = $("<li><a>Machine - " + ip + "</a></li>");
		this.window_el = $("<div></div>");
		this.window_el.append("<h2>Machine - " + ip + "</h2>");
		this.window_el.append("<h3>Time: <span id=\"time\">0</span> hrs.")
		this.window_el.append("<div class=\"status-label\">Status: <span id=\"status\"></span></div>");
		this.window_el.append("<button type=\"button\" class=\"btn btn-primary\" id=\"trigger-failure\">Report Machine Failure</button>")
		this.window_el.append("<div class=\"well\" id=\"console\"></div>");
		this.ws = new WebSocket("ws://"+ip+":9091/");
		
		this.appendLog = function(log){
			var console_el = this.window_el.find("#console");
			console_el.append(log+"<br/>");
			console_el.scrollTop(console_el.scrollHeight);
			
		};
		this.setStatus = function(status_code){
			var el = this.window_el.find("#status");
			el.text(statusList[status_code]);
			if(status_code == "26")
				this.window_el.find("#trigger-failure").prop("disabled",false);
			else 
				this.window_el.find("#trigger-failure").prop("disabled", true);
		};
		this.window_el.find("#trigger-failure").click(function() {
			context.ws.send("failure");
		});
		this.ws.onopen = function()
		{
			$("#machine-list").append(context.list_el);
			$("#machine-window").append(context.window_el);
			context.setStatus("25");
			context.window_el.hide();
		};
			
		this.ws.onmessage = function (evt) 
		{ 
			var message = JSON.parse(evt.data);
			if(message.type == "log"){
				context.appendLog(message.log);
				context.setStatus(message.status_code);
			}
			else if(message.type == "time_stamp"){
				context.window_el.find("#time").text(message.time);
			}
		}
			
		this.ws.onclose = function()
		{ 
			console.log("closing connection");
			//context.list_el.remove();
			//context.window_el.remove();
		};
		this.ws.onerror = function()
		{
			
		};
		
		context.list_el.click(function() {
			for(var ip in machines){
				machines[ip].window_el.hide();
			}
			 context.window_el.show();
		});
		
	}
});
