Spine = @Spine or require('spine')

class DialogController extends Spine.Controller
  elements:
    'form': 'form'
  
  events:
    'dialogclose .repository-form': 'close'
    'change .endisable': 'processEndisable'
    'submit form': 'submit'
    'click .button': 'processFormButton'
    
  processFormButton: (e) =>
    # NOP

  delete: (e) =>
    @log("Deleting: #{@item}")
    @deleteDialog = require('views/delete.dialog')(@item)
    $(@deleteDialog).dialog(
      title: "Delete",
      autoOpen: true,
      modal: true,
      buttons:
        Yes: =>
          @deleting() if @deleting
          @owner.deleting(@item) if @owner and @owner.deleting
          @item.destroy()
        
          $(@deleteDialog).dialog('close')
          $(@el).dialog('close')
        No: =>
          $(@deleteDialog).dialog('close')
    )
    
  close: (e) =>
    @log( "close")
    @dialogClosed( e )
    $(@el).dialog('close')
  
  dialogClosed: (e) =>
    @log( "dialogClosed")
    e.preventDefault()
    if !@isSaved
      @canceling( e ) if @canceling
      @owner.cancelled(@item) if @owner and @owner.cancelled
      @cleanup()
  
  cleanup: =>
    # @item = null
    # @owner = null
    # @editing = null
    
  open: (item, editing, owner) =>
    @owner = owner
    @editing = editing
    @opening(item) if @opening
    @change(item)
  
  change: (item) =>
    @item = item
    @render()
  
  renderDialog: (title, width, height) =>
    params =
      title: "#{if @editing then 'Edit' else 'New'} #{title}"
      autoOpen: true
      height: width
      width: height
      modal: true
      # resizable: false
      close: @dialogClosed
      buttons:
        Save: (e) =>
          @submit(e)
        Cancel: (e) =>
          @close(e)
        Delete: (e) =>
          @delete(e)
      
    $(@el).dialog(params)
    @initEndisables()
    
    $(@el).find('input.name-field').attr('size', '15')
    $(@el).find('input[type=number]').attr('size', '4')
    $(@el).find('input[type=url]').attr('size', '50')
    
    # @log($(@el).html())
    # $(@el).html()
  
  initEndisables: =>
    $(@el).find('.endisable').each( (i,target) =>
      @endisable(target)
    )
  
  processEndisable: (e) =>
    @endisable(e.target)
  
  endisable: (target) =>
    name = $(target).attr('name')
    affected = $(target).attr('affects')
    return unless affected
    
    affected = affected.split(' ')
    checked = $(@el).find(".endisable[name=#{name}]:checked")
    
    for a in affected
      reverse = false
      if ( a.substring(0,1) == '!' )
        a = a.substring(1)
        reverse = true
        
      affected = $(@el).find("[name=#{a}]")
      if reverse != ( checked.length == 0 )
        $(affected).hide()
      else
        $(affected).show()
  
  submit: (e) =>
    @log( "submit" )
    @isSaved = true
    @log("Item is: #{JSON.stringify(@item)}")
    e.preventDefault()
    $(@el).dialog('close')
    @saving( e ) if @saving
    @owner.saved(@item) if @owner and @owner.saved
    @cleanup()
  
  saving: (e) =>
    @log( "saving" )
    if @customFromForm
      @log( "[CUSTOM save]")
      @customFromForm( @item, @form )
    else
      @log( "[FROM-FORM save]")
      @item.fromForm(@form)
    
    if @doctype
      @item.doctype = @doctype
      @item.key = "#{@doctype}:#{@item.name}" if @item.name
      
    @item.id = @item.name if @item.name
    
    @log("Before saving: #{JSON.stringify(@item)}")
    
    @item.save()
    
    @log("After saving: #{JSON.stringify(@item)}")
    @log( "Item key: #{@item.key}, name: #{@item.name}")

module?.exports = DialogController