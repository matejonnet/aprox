Spine = require('spine')

class RemoteRepository extends Spine.Model
  @configure 'RemoteRepository', 'name', 'remote_url', 'timeout_seconds', 
             'is_passthrough', 'cache_timeout_seconds', 'key_certificate_pem', 
             'key_password', 'server_certificate_pem', 'proxy_host', 
             'proxy_port', 'proxy_user', 'proxy_password'
             
  @extend Spine.Model.Ajax
  
  @url: window.location.pathname + 'api/1.0/admin/remotes'
  
  @fromJSON: (objects) ->
    return unless objects

    if typeof objects is 'string'
      objects = JSON.parse(objects)
      
      
    if objects.items
      objects = objects.items
      if objects
        for object in objects
          if object
            object.remote_url = object.url if object.url
            
            if object.key
              key = object.key
              parts = key.split(':')
              object.name = parts[1] if parts and parts.length > 1
              object.type = parts[0] if parts and parts.length > 0
              object.id = object.name
            
            object.key = "#{object.key.type}:#{object.key.name}" if object.key and object.key.type and object.key.name
            
            console.log(JSON.stringify(object))
    else
      object = objects
      object.remote_url = object.url if object.url
      
      if object.key
        key = object.key
        parts = key.split(':')
        object.name = parts[1] if parts and parts.length > 1
        object.type = parts[0] if parts and parts.length > 0
        object.id = object.name
      
      object.key = "#{object.key.type}:#{object.key.name}" if object.key and object.key.type and object.key.name
      
      console.log(JSON.stringify(object))
      

    if Spine.isArray(objects)
      (new @(value) for value in objects)
    else
      new @(objects)
  
  toJSON: (objects) ->
    data = @attributes()
    
    result = null
    if Spine.isArray(data)
      objs = []
      for obj in data
        if obj
          o =
            'id': obj.name
            'name': obj.name
            'url': obj.remote_url
            'timeout_seconds': parseInt obj.timeout_seconds
            'is_passthrough': if obj.is_passthrough is 'on' then true else false
            'cache_timeout_seconds': if obj.is_passthrough is true then -1 else parseInt obj.cache_timeout_seconds
            'key_certificate_pem': obj.key_certificate_pem if obj.key_certificate_pem and obj.key_certificate_pem.length > 0
            'key_password': obj.key_password if obj.key_password and obj.key_password.length > 0
            'server_certificate_pem': obj.server_certificate_pem if obj.server_certificate_pem and obj.server_certificate_pem.length > 0
            'proxy_host': obj.proxy_host if obj.proxy_host and obj.proxy_host.length > 0
            'proxy_port': parseInt obj.proxy_port if obj.proxy_port
            'proxy_user': obj.proxy_user if obj.proxy_user and obj.proxy_user.length > 0
            'proxy_password': obj.proxy_password if obj.proxy_password and obj.proxy_password.length > 0
            'key': "remote:#{obj.name}"
            
          objs.push(o)
      
      data =
        'items': objs
    else
      if data
        data = 
          'id': data.name
          'name': data.name
          'url': data.remote_url
          'timeout_seconds': parseInt data.timeout_seconds
          'is_passthrough': if data.is_passthrough is 'on' then true else false
          'cache_timeout_seconds': if data.is_passthrough is true then -1 else parseInt data.cache_timeout_seconds
          'key_certificate_pem': data.key_certificate_pem if data.key_certificate_pem and data.key_certificate_pem.length > 0
          'key_password': data.key_password if data.key_password and data.key_password.length > 0
          'server_certificate_pem': data.server_certificate_pem if data.server_certificate_pem and data.server_certificate_pem.length > 0
          'proxy_host': data.proxy_host if data.proxy_host and data.proxy_host.length > 0
          'proxy_port': parseInt data.proxy_port if data.proxy_port
          'proxy_user': data.proxy_user if data.proxy_user and data.proxy_user.length > 0
          'proxy_password': data.proxy_password if data.proxy_password and data.proxy_password.length > 0
          'key': "remote:#{data.name}"
    
    data
  
module.exports = RemoteRepository