<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://sakaiproject.org/jsf2/sakai" prefix="sakai" %>
<%@ taglib uri="http://sakaiproject.org/jsf/messageforums" prefix="mf" %>

<jsp:useBean id="msgs" class="org.sakaiproject.util.ResourceLoader" scope="session">
   <jsp:setProperty name="msgs" property="baseName" value="org.sakaiproject.api.app.messagecenter.bundle.Messages"/>
</jsp:useBean>

<f:view>
  <sakai:view toolCssHref="/messageforums-tool/css/msgcntr-qtip.css" title="#{msgs.pvt_detmsgreply}">
    <h:form id="pvtMsgDetail" onsubmit="getShadowTags('pvtMsgDetail')">
    		<script>
       			// Define i18n for js text
       			var msgs_js = { 
       				"loading_wait":<h:outputText value="\"#{msgs.loading_wait}\"" />,
       				"cdfm_profile_information":<h:outputText value="\"#{msgs.cdfm_profile_information}\"" />
       			};
       		</script>
           	<script>includeLatestJQuery("msgcntr");</script>
           	<script>includeWebjarLibrary("qtip2");</script>
       		<script src="/messageforums-tool/js/sak-10625.js"></script>
       		<script src="/messageforums-tool/js/messages.js"></script>
			<script>
				$(document).ready(function () {
					var menuLink = $('#messagesMainMenuLink');
					var menuLinkSpan = menuLink.closest('span');
					menuLinkSpan.addClass('current');
					menuLinkSpan.html(menuLink.text());
					
					$('#pvtMsgDetail .authorProfile').each(function() {
						$(this).qtip({ 
							content: {
								ajax: {
									url: $(this).prop('href'),
									type: 'GET'
								}
							},
							position: {	my: 'left center', at: 'top center'},
							show: { event: 'click', solo: true, effect: {length:0} },
							hide: { when:'unfocus', fixed:true, delay: 300,  effect: {length:0} },
							style: { classes: 'msgcntr-profile-qtip' }
						});
						$(this).prop('href', 'javascript:;');
					});
				});
			</script>
			<%@ include file="/jsp/privateMsg/pvtMenu.jsp" %>
<!--jsp/privateMsg/pvtMsgDetail.jsp-->
<%--			<sakai:tool_bar_message value="#{msgs.pvt_detmsgreply}" /> --%> 
			<h:messages styleClass="alertMessage" id="errorMessages" rendered="#{! empty facesContext.maximumSeverity}"/> 
            <h:outputText value="#{PrivateMessagesTool.multiDeleteSuccessMsg}" styleClass="sak-banner-success" rendered="#{PrivateMessagesTool.multiDeleteSuccess}" />

<h:panelGrid columns="2" width="100%" styleClass="navPanel specialLink">
	<h:panelGroup>
		<f:verbatim><div class="breadCrumb"><h3></f:verbatim>
<%--   			  <h:commandLink action="#{PrivateMessagesTool.processActionHome}" value="#{msgs.cdfm_message_forums}" title=" #{msgs.cdfm_message_forums}"/><h:outputText value=" " /><h:outputText value=" / " /><h:outputText value=" " />
  			  <h:commandLink action="#{PrivateMessagesTool.processActionPrivateMessages}" value="#{msgs.cdfm_message_pvtarea}" title=" #{msgs.cdfm_message_pvtarea}"/><h:outputText value=" " /><h:outputText value=" / " /><h:outputText value=" " />
--%>
		<h:panelGroup rendered="#{PrivateMessagesTool.messagesandForums}" >
			<h:commandLink action="#{PrivateMessagesTool.processActionHome}" value="#{msgs.cdfm_message_forums}" title="#{msgs.cdfm_message_forums}"/>
			<h:outputText value=" / " />
		</h:panelGroup>
		<h:commandLink action="#{PrivateMessagesTool.processActionPrivateMessages}" value="#{msgs.pvt_message_nav}" title=" #{msgs.cdfm_message_forums}"/>
		<h:outputText value=" " /><h:outputText value=" / " /><h:outputText value=" " />
		<f:verbatim><h:outputText value=" " /><h:outputText value=" / " /><h:outputText value=" " /></f:verbatim>
		<h:commandLink rendered="#{(PrivateMessagesTool.msgNavMode == 'pvt_received' || PrivateMessagesTool.msgNavMode == 'pvt_sent' || PrivateMessagesTool.msgNavMode == 'pvt_deleted' || PrivateMessagesTool.msgNavMode == 'pvt_drafts')}"
				action="#{PrivateMessagesTool.processDisplayForum}" value="#{msgs[PrivateMessagesTool.msgNavMode]}" title=" #{msgs[PrivateMessagesTool.msgNavMode]}"/>
		<h:outputText rendered="#{(PrivateMessagesTool.msgNavMode == 'pvt_received' || PrivateMessagesTool.msgNavMode == 'pvt_sent' || PrivateMessagesTool.msgNavMode == 'pvt_deleted' || PrivateMessagesTool.msgNavMode == 'pvt_drafts')}" value=" / " />
		<h:outputText value="#{PrivateMessagesTool.detailMsg.msg.title}" />
		<f:verbatim></h3></div></f:verbatim>
	</h:panelGroup>

	<h:panelGroup styleClass="itemNav " rendered="#{!PrivateMessagesTool.detailMsg.isPreview && !PrivateMessagesTool.detailMsg.isPreviewReply && !PrivateMessagesTool.detailMsg.isPreviewReplyAll && !PrivateMessagesTool.detailMsg.isPreviewForward}">
		<h:panelGroup rendered="#{!PrivateMessagesTool.detailMsg.hasPre}" styleClass="button formButtonDisabled">
			<h:outputText value="#{msgs.pvt_prev_msg}"   />
		</h:panelGroup>
		<h:commandLink action="#{PrivateMessagesTool.processDisplayPreviousMsg}" value="#{msgs.pvt_prev_msg}"  
						rendered="#{PrivateMessagesTool.detailMsg.hasPre}" title=" #{msgs.pvt_prev_msg}" styleClass="btn-primary" >
		</h:commandLink>
		
		<h:panelGroup styleClass="button formButtonDisabled" rendered="#{!PrivateMessagesTool.detailMsg.hasNext}">
			<h:outputText value="#{msgs.pvt_next_msg}"  />
		</h:panelGroup>
		<h:commandLink action="#{PrivateMessagesTool.processDisplayNextMsg}" value="#{msgs.pvt_next_msg}" 
					rendered="#{PrivateMessagesTool.detailMsg.hasNext}" title=" #{msgs.pvt_next_msg}" styleClass="btn-primary" >
		</h:commandLink>
	</h:panelGroup>
	
      </h:panelGrid>
      <f:verbatim>
      <h3>
      </f:verbatim>
      <h:outputText value="#{msgs.pvt_preview}"  styleClass="h1" rendered="#{PrivateMessagesTool.detailMsg.isPreview || PrivateMessagesTool.detailMsg.isPreviewReply || PrivateMessagesTool.detailMsg.isPreviewReplyAll || PrivateMessagesTool.detailMsg.isPreviewForward }"/>
      <f:verbatim>
      </h3>
      </f:verbatim>

        <sakai:button_bar rendered="#{!PrivateMessagesTool.deleteConfirm && !PrivateMessagesTool.detailMsg.isPreview && !PrivateMessagesTool.detailMsg.isPreviewReply && !PrivateMessagesTool.detailMsg.isPreviewReplyAll && !PrivateMessagesTool.detailMsg.isPreviewForward}" >
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgReply}" value="#{msgs.pvt_repmsg}" accesskey="r" />
            <%--SAK-10505 add forward --%>
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgReplyAll}" value="#{msgs.pvt_repmsg_ALL}" accesskey="r" /><h:commandButton action="#{PrivateMessagesTool.processPvtMsgForward}" value="#{msgs.pvt_forwardmsg}" accesskey="r"/>
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgMove}" value="#{msgs.pvt_move}" accesskey="m" />
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPublishToFaqEdit}" value="#{msgs.pvt_publish_to_faq}"
                    rendered="#{PrivateMessagesTool.detailMessagePublishableToFaq && mfPublishToFaqBean.canPost}" accesskey="m" />
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgDeleteConfirm}" value="#{msgs.pvt_delete}"  />
        </sakai:button_bar>

        <sakai:button_bar rendered="#{PrivateMessagesTool.deleteConfirm}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgDeleteConfirmYes}" value="#{msgs.pvt_delete}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgCancelToDetailView}" value="#{msgs.pvt_cancel}" accesskey="x" />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreview}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewSend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreviewReply}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplySend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplyBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreviewReplyAll}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplyAllSend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplyAllBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreviewForward}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewForwardSend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewForwardBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
          
         
         
        <div class="container-fluid">
            <div class="row">
                <div class="col-md-2">
                    <%-- author image --%>
                    <f:subview id="authorImage" rendered="#{PrivateMessagesTool.showProfileInfoMsg}">
                        <h:panelGroup styleClass="authorImage">
                            <h:outputLink value="#{PrivateMessagesTool.serverUrl}/direct/portal/#{PrivateMessagesTool.detailMsg.msg.authorId}/formatted" styleClass="authorProfile" rendered="#{PrivateMessagesTool.showProfileLink}">
                                <h:graphicImage value="#{PrivateMessagesTool.serverUrl}/direct/profile/#{PrivateMessagesTool.detailMsg.msg.authorId}/image/thumb" alt="#{message.message.author}" />
                            </h:outputLink>
                            <h:graphicImage value="#{PrivateMessagesTool.serverUrl}/direct/profile/#{PrivateMessagesTool.detailMsg.msg.authorId}/image/thumb" alt="#{message.message.author}" rendered="#{!PrivateMessagesTool.showProfileLink}"/>
                        </h:panelGroup>
                    </f:subview>
                </div>
                <div class="col-md-10">
                    <table class="itemSummary">
                    <tr>
                        <th>
                            <h:outputText value="#{msgs.pvt_authby}"/>
                        </th>
                        <td>
                            <h:outputText value="#{PrivateMessagesTool.detailMsg.msg.author}" />
                            <h:outputText value=" #{msgs.pvt_openb}" />
                            <h:outputText value="#{PrivateMessagesTool.detailMsg.msg.created}" >
                                <f:convertDateTime pattern="#{msgs.date_format}" timeZone="#{PrivateMessagesTool.userTimeZone}" locale="#{PrivateMessagesTool.userLocale}"/>
                            </h:outputText>
                            <h:outputText value=" #{msgs.pvt_closeb}" />
                        </td>
                    </tr>
                    <tr>
                        <th>
                            <h:outputText value="#{msgs.pvt_group}"/>
                        </th>
                        <td>
                            <h:outputText value="#{PrivateMessagesTool.senderRoleAndGroups} "/>
                        </td>
                    </tr>
                    <tr>
                        <th>
                            <h:outputText value="#{msgs.pvt_to}" />
                        </th>
                        <td>
                            <h:outputText value="#{PrivateMessagesTool.detailMsg.recipientsAsText}" />
                            <h:outputText value=", (#{msgs.pvt_sent_as_email})" rendered="#{PrivateMessagesTool.detailMsg.msg.externalEmail}"/>
                        </td>
                    </tr>
                    <f:subview id="bccRecipients" rendered="#{ForumTool.userId == PrivateMessagesTool.detailMsg.msg.createdBy && PrivateMessagesTool.detailMsg.recipientsAsTextBcc != null && PrivateMessagesTool.detailMsg.recipientsAsTextBcc != ''}">
                    <f:verbatim>
                    <tr>
                        <th>
                    </f:verbatim>
                        <h:outputText value="#{msgs.pvt_bcc}" />
                    <f:verbatim>
                        </th>
                        <td>
                    </f:verbatim>
                        <h:outputText value="#{PrivateMessagesTool.detailMsg.recipientsAsTextBcc}" />
                    <f:verbatim>
                        </td>
                    </tr>
                    </f:verbatim>
                    </f:subview>
                    <tr>
                        <th>
                            <h:outputText value="#{msgs.pvt_subject}"/>
                        </th>
                        <td>
                            <h:outputText value="#{PrivateMessagesTool.detailMsg.msg.title}" />  
                        </td>
                    </tr>
                    <tr>
                        <th>
                            <h:outputText value="#{msgs.pvt_label} "/>
                        </th>
                        <td>
                            <h:outputText value="#{PrivateMessagesTool.detailMsg.label}" />  
                        </td>
                    </tr> 
                    <tr>
                        <th>
                            <h:outputText value="#{msgs.pvt_att}" rendered="#{!empty PrivateMessagesTool.detailMsg.attachList}" />
                        </th>
                        <td>
                            <%-- Attachments --%>
                            <%-- gsilver:copying the rendered attribute from the column to the dataTable - do not want a table rendered without children (is not xhtml)--%>
                            <h:dataTable value="#{PrivateMessagesTool.detailMsg.attachList}" 
                                        var="eachAttach"  styleClass="attachListJSF"  rendered="#{!empty PrivateMessagesTool.detailMsg.attachList}">
                                <h:column rendered="#{!empty PrivateMessagesTool.detailMsg.attachList}">
                                    <sakai:contentTypeMap fileType="#{eachAttach.attachment.attachmentType}" mapType="image" var="imagePath" pathPrefix="/library/image/"/>                                 
                                        <h:graphicImage id="exampleFileIcon" value="#{imagePath}" />                                  
                                        <%--<h:outputLink value="#{eachAttach.attachmentUrl}" target="_blank">
                                            <h:outputText value="#{eachAttach.attachmentName}"/>
                                            </h:outputLink>--%>
                                    <h:outputLink value="#{eachAttach.url}" target="_blank">
                                        <h:outputText value="#{eachAttach.attachment.attachmentName}"/>
                                    </h:outputLink>
                                </h:column>
                            </h:dataTable>   
                            <%-- Attachments --%>
                        </td>
                    </tr>
                    </table>
                </div>
            </div>
        </div>

        <h:panelGroup rendered="#{PrivateMessagesTool.canUseTags}">
          <h4><h:outputText value="#{msgs.pvt_tags_header}" /></h4>
          <h:inputText value="#{PrivateMessagesTool.selectedTags}" styleClass="hidden" id="tag_selector"></h:inputText>
          <h:panelGroup styleClass="#{PrivateMessagesTool.detailMsg.isPreview || PrivateMessagesTool.detailMsg.isPreviewReply || PrivateMessagesTool.detailMsg.isPreviewReplyAll || PrivateMessagesTool.detailMsg.isPreviewForward ? 'DisableTags' : ''}">
            <sakai-tag-selector
              <h:panelGroup rendered="#{PrivateMessagesTool.detailMsg.isPreview || PrivateMessagesTool.detailMsg.isPreviewReply || PrivateMessagesTool.detailMsg.isPreviewReplyAll || PrivateMessagesTool.detailMsg.isPreviewForward}">
                tabindex="-1" 
              </h:panelGroup>
              selected-temp='<h:outputText value="#{PrivateMessagesTool.selectedTags}"/>'
              collection-id='<h:outputText value="#{PrivateMessagesTool.getUserId()}"/>'
              item-id='<h:outputText value="#{PrivateMessagesTool.detailMsg.msg.id}"/>'
              site-id='<h:outputText value="#{PrivateMessagesTool.getSiteId()}"/>'
              tool='<h:outputText value="#{PrivateMessagesTool.getTagTool()}"/>'
              add-new="true"
            ></sakai-tag-selector>
          </h:panelGroup>
          <h:panelGroup rendered="#{!PrivateMessagesTool.detailMsg.isPreview && !PrivateMessagesTool.detailMsg.isPreviewReply && !PrivateMessagesTool.detailMsg.isPreviewReplyAll && !PrivateMessagesTool.detailMsg.isPreviewForward}">
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgSaveTags}" value="#{msgs.pvt_tags_save}"  />
          </h:panelGroup>
        </h:panelGroup>

		<hr class="itemSeparator" />
		
        	  <mf:htmlShowArea value="#{PrivateMessagesTool.detailMsg.msg.body}" id="htmlMsgText" hideBorder="true" />
        
        <sakai:button_bar rendered="#{!PrivateMessagesTool.deleteConfirm && !PrivateMessagesTool.detailMsg.isPreview && !PrivateMessagesTool.detailMsg.isPreviewReply && !PrivateMessagesTool.detailMsg.isPreviewReplyAll && !PrivateMessagesTool.detailMsg.isPreviewForward}" >
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgReply}" value="#{msgs.pvt_repmsg}" accesskey="r"/>
            <%--SAKAI-10505 add forward--%>
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgReplyAll}" value="#{msgs.pvt_repmsg_ALL}" accesskey="r" /><h:commandButton action="#{PrivateMessagesTool.processPvtMsgForward}" value="#{msgs.pvt_forwardmsg}" accesskey="r"/>
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgMove}" value="#{msgs.pvt_move}" accesskey="m" />
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPublishToFaqEdit}" value="#{msgs.pvt_publish_to_faq}"
                    rendered="#{PrivateMessagesTool.detailMessagePublishableToFaq && mfPublishToFaqBean.canPost}" accesskey="m" />
            <h:commandButton action="#{PrivateMessagesTool.processPvtMsgDeleteConfirm}" value="#{msgs.pvt_delete}"  />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.deleteConfirm}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgDeleteConfirmYes}" value="#{msgs.pvt_delete}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgCancelToDetailView}" value="#{msgs.pvt_cancel}" accesskey="x" />
        </sakai:button_bar>
		<sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreview}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewSend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreviewReply}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplySend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplyBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreviewReplyAll}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplyAllSend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewReplyAllBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
        <sakai:button_bar rendered="#{PrivateMessagesTool.detailMsg.isPreviewForward}" >
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewForwardSend}" value="#{msgs.pvt_send}" accesskey="s" styleClass="active"/>
          <h:commandButton action="#{PrivateMessagesTool.processPvtMsgPreviewForwardBack}" value="#{msgs.pvt_back}" accesskey="b" />
        </sakai:button_bar>
        
      </h:form>

  </sakai:view>
</f:view> 
                    
