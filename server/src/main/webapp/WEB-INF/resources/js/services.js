'use strict';

/* Services */

var AppServices = angular.module('SpringAngularApp.services', []);

AppServices.value('version', '0.1');

//AppServices.service('accessTokenCookieService', function($cookies, $cookieStore){
//	return ({
//		initToken: initToken,
//		removeToken: removeToken,
//		initCookies: initCookies,
//		removeCookies: removeCookies
//	});
//	
//	function initToken(token){
//		httpHeaders.common['Authorization'] = token || $cookies.token;
//	};
//	
//	function initCookies() {
//		var jsessionid = $cookies.JSESSIONID;
//		$cookieStore.put('JSESSIONID', jsessionid);
//		console.log("Add Cookies");
//	};
//	
//	function removeToken() {
//		httpHeaders.common['Authorization'] = null;
//	};
//	
//	function removeCookies() {
//		$cookieStore.remove('JSESSIONID');
//		delete $cookies.JSESSIONID;
//		console.log("Renmove Cookies");
//	};
//});

AppServices.service('mainRequestFactory', function($http, $q) {
	var mainRequestFactory = {};
	
	mainRequestFactory.getMainRequestObjects = function() {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'home/mainRequestObjects'
		});
		return (request.then(mainRequestFactory.handleSucess, mainRequestFactory.handleError));
	};
	
	mainRequestFactory.handleSuccess = function(response){
		return(response.data);
	};
	
	mainRequestFactory.handleError = function( response ){
		if(!angular.isObject(response.data) || !response.data.message){
			return($q.reject("Request failed"));
		}
		
		return($q.reject(response.data));
	};
	
	return mainRequestFactory;
});

AppServices.factory('serverTransactionsFactory', function($http, $q) {
	var serverTransactionsFactory = {};
	
	serverTransactionsFactory.getLastAccountTransactions = function(id) {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'serveraccount/lastAccountTransaction/' + id
		});
		return (request.then(serverTransactionsFactory.handleSucess, serverTransactionsFactory.handleError));
	};
	
	serverTransactionsFactory.getHistory = function() {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: "history/transactions"
		});
		return (request.then(serverTransactionsFactory.handleSuccess, serverTransactionsFactory.handleError));
	};
	
	serverTransactionsFactory.handleSuccess = function(response){
		return(response.data);
	};
	
	serverTransactionsFactory.handleError = function( response ){
		if(!angular.isObject(response.data) || !response.data.message){
			return($q.reject("Request failed"));
		}
		
		return($q.reject(response.data));
	};
	
	return serverTransactionsFactory;
});

AppServices.factory('userAccountFactory', function($http, $q, transformRequestAsFormPost) {
	var userAccountFactory = {};
	
	userAccountFactory.getAdmins = function() {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'home/admins'
		});
		return (request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};
	
	userAccountFactory.getUsers = function() {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'users/all'
		});
		return (request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};
	
	userAccountFactory.updateMail = function(user){
		
		var request = $http({
			method: 'POST',
			url: 'home/updateMail',
			headers: { 'Content-Type': 'application/json;charset=utf-8'},
			transformRequest: function(){
				angular.isObject(data) && String(data) !== '[object File]' ? transformRequestAsFormPost : data;
			},
			data: {"username":user.username,
					"email":user.email
					}
		});
		return(request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};
	
	userAccountFactory.updatePassword = function(user){
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json;charset=utf-8'},
			url: 'home/updatePassword',
			data: {"username":user.username,"password":user.password}
		});
		return(request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};

	userAccountFactory.getLoggedUser = function(){
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json;charset=utf-8'},
			url: 'home/userAccount'
		});
		return(request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};
	
	userAccountFactory.inviteAdmin = function(user){
		var request = $http({
			method: 'POST',
			url: 'home/inviteAdmin',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			params:{
				"email": user.email
			}
		});
		return(request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};
	
	userAccountFactory.sendMailToAll = function(emailContent){
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'users/sendMailToAll',
			params:{
				"subject": emailContent.subject,
				"text": emailContent.text
			}
		});
		return(request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};
	
	userAccountFactory.handleSuccess = function( response ){
		return(response.data);
	};
	
	userAccountFactory.handleError = function( response ){
		if(!angular.isObject(response.data) || !response.data.message){
			return($q.reject("Request failed"));
		}
		
		return($q.reject(response.data));
	};
	
	return userAccountFactory;
});

AppServices.factory('serverAccountFactory', function($http, $q) {
	var serverAccountFactory = {};
	
	serverAccountFactory.getAccounts = function() {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json;charset=utf-8'},
			url: 'relation/accounts'
		});
		return (request.then(serverAccountFactory.handleSuccess, serverAccountFactory.handleError));
	};
	
	serverAccountFactory.getServerAccount = function(id){
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'serveraccount/account/' + id
		});
		return (request.then(serverAccountFactory.handleSuccess, serverAccountFactory.handleError));
	};
	
	serverAccountFactory.createNewAccount = function(url){
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json;charset=utf-8'},
			url: 'relation/createNewAccount',
			params:{"url":url.toString()}
		});
		return(request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError));
	};
	
	serverAccountFactory.deletedAccount = function(url){
		var request= $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'serveraccount/deleteAccount',
			params:{
				"url": url
			}
		});
		request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError);
	};
	
	serverAccountFactory.updateTrustLevel = function(serverAccount, trustLevel){
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'serveraccount/updateTrustLevel',
			params: {
				"url": serverAccount.url,
				"oldLevel": serverAccount.trustLevel,
				"newLevel": trustLevel
			}
		});
		request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError);
	};
	
	serverAccountFactory.updateBalanceLimit  = function(serverAccount, balanceLimit){
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'serveraccount/updateBalanceLimit',
			params: {
				"url": serverAccount.url,
				"oldLimit": serverAccount.balanceLimit,
				"newLimit": balanceLimit
			}
		});
		request.then(userAccountFactory.handleSuccess, userAccountFactory.handleError);
	};
	
	serverAccountFactory.handleSuccess = function(response){
		return(response.data);
	};
	
	serverAccountFactory.handleError = function(response){
		if(!angular.isObject(response.data) || !response.data.message){
			return($q.reject("Request failed"));
		}
		
		return($q.reject(response.data));
	};
	
	return serverAccountFactory;
});

AppServices.factory('activitiesFactory', function($http, $q) {
	var activitiesFactory = {};
	
	activitiesFactory.getActivities = function() {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'activities/logs'
		});
		return (request.then(activitiesFactory.handleSuccess, activitiesFactory.handleError));
	};
	
	activitiesFactory.handleSuccess = function( response ){
		return(response.data);
	};
	
	activitiesFactory.handleError = function( response ){
		if(!angular.isObject(response.data) || !response.data.message){
			return($q.reject("Request failed"));
		}
		
		return($q.reject(response.data));
	};
	
	return activitiesFactory;
});

AppServices.factory('messagesFactory', function($http, $q) {
	var messagesFactory = {};
	
	messagesFactory.getMessages = function() {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'messages/all'
		});
		return (request.then(messagesFactory.handleSuccess, messagesFactory.handleError));
	};

	messagesFactory.accept = function(updatedAccount) {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'messages/accept',
			data: {
				"url": updatedAccount.url,
				"trustLevel": updatedAccount.trustLevel
			}
		});
		return (request.then(messagesFactory.handleSuccess, messagesFactory.handleError));
	};

	messagesFactory.decline = function(updatedAccount) {
		var request = $http({
			method: 'POST',
			headers: { 'Content-Type': 'application/json; charset=UTF-8'},
			url: 'messages/decline',
			data: {
				"url": updatedAccount.url,
				"trustLevel": updatedAccount.trustLevel
			}
		});
		return (request.then(messagesFactory.handleSuccess, messagesFactory.handleError));
	};
	
	
	messagesFactory.handleSuccess = function( response ){
		return(response.data);
	};
	
	messagesFactory.handleError = function( response ){
		if(!angular.isObject(response.data) || !response.data.message){
			return($q.reject("Request failed"));
		}
		
		return($q.reject(response.data));
	};
	
	return messagesFactory;
});

//TODO: mehmet set link http://wemadeyoulook.at/en/blog/implementing-basic-http-authentication-http-requests-angular/
AppServices.factory('base64Factory', function() {
	
	var base64Factory = {};
	
	var keyStr = 'ABCDEFGHIJKLMNOP' +
        'QRSTUVWXYZabcdef' +
        'ghijklmnopqrstuv' +
        'wxyz0123456789+/' +
        '=';
    
	base64Factory.encode = function (input) {
        var output = "";
        var chr1, chr2, chr3 = "";
        var enc1, enc2, enc3, enc4 = "";
        var i = 0;

        do {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output = output +
                keyStr.charAt(enc1) +
                keyStr.charAt(enc2) +
                keyStr.charAt(enc3) +
                keyStr.charAt(enc4);
            chr1 = chr2 = chr3 = "";
            enc1 = enc2 = enc3 = enc4 = "";
        } while (i < input.length);

        return output;
	};
	
	base64Factory.decode = function (input) {
        var output = "";
        var chr1, chr2, chr3 = "";
        var enc1, enc2, enc3, enc4 = "";
        var i = 0;

        // remove all characters that are not A-Z, a-z, 0-9, +, /, or =
        var base64test = /[^A-Za-z0-9\+\/\=]/g;
        if (base64test.exec(input)) {
            alert("There were invalid base64 characters in the input text.\n" +
                "Valid base64 characters are A-Z, a-z, 0-9, '+', '/',and '='\n" +
                "Expect errors in decoding.");
        }
        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        do {
            enc1 = keyStr.indexOf(input.charAt(i++));
            enc2 = keyStr.indexOf(input.charAt(i++));
            enc3 = keyStr.indexOf(input.charAt(i++));
            enc4 = keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64) {
                output = output + String.fromCharCode(chr3);
            }

            chr1 = chr2 = chr3 = "";
            enc1 = enc2 = enc3 = enc4 = "";

        } while (i < input.length);

        return output;
    };
    
    return base64Factory;
});

AppServices.factory("transformRequestAsFormPost",function() {
		 
	// I prepare the request data for the form post.
	function transformRequest( data) {
		return( param( data ) );
	}
		 
		 
	// Return the factory value.
	return( transformRequest );
	
	function param(obj) {
		var query = '', name, value, fullSubName, subName, subValue, innerObj, i;  
	    for(name in obj) {
	      value = obj[name];
	        
	      if(value instanceof Array) {
	        for(i=0; i<value.length; ++i) {
	          subValue = value[i];
	          fullSubName = name + '[' + i + ']';
	          innerObj = {};
	          innerObj[fullSubName] = subValue;
	          query += param(innerObj) + '&';
	        }
	      }
	      else if(value instanceof Object) {
	        for(subName in value) {
	          subValue = value[subName];
	          fullSubName = name + '[' + subName + ']';
	          innerObj = {};
	          innerObj[fullSubName] = subValue;
	          query += param(innerObj) + '&';
	        }
	      }
	      else if(value !== undefined && value !== null)
	        query += encodeURIComponent(name) + '=' + encodeURIComponent(value) + '&';
	    }
	      
	    return query.length ? query.substr(0, query.length - 1) : query;
	  };
		 
});