package raptor.alias;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import raptor.Raptor;
import raptor.chat.ChatEvent;
import raptor.chat.ChatType;
import raptor.connector.MessageCallback;
import raptor.swt.chat.ChatConsoleController;
import raptor.util.RaptorStringTokenizer;

public class GrantSpoofAlias extends RaptorAlias {

	public static List<String> usersWithControl = new ArrayList<String>(10);

	public GrantSpoofAlias() {
		super(
				"grantspoof",
				"Allows another user to spoof commands on your account. Use with care! "
						+ "A user could be very disruptive with your account by fooling you into typing this. "
						+ "After you give another user control every tell the user sends you will be "
						+ "executed as a command to the server. If the user sends you a command that "
						+ "starts with / or \\ the command will be ignored."
						+ "To remove the users access type 'grantspoof remove'.",
				"'grantspoof [remove | userName]'. Example: 'grantspoof CDay' "
						+ "afterwards 'givecontrol remove' to remove control.");
	}

	@Override
	public RaptorAliasResult apply(final ChatConsoleController controller,
			String command) {
		if (command.startsWith("grantspoof")) {
			RaptorStringTokenizer tok = new RaptorStringTokenizer(command, " ",
					true);
			tok.nextToken();
			final String param = tok.nextToken();

			if (param == null) {
				return new RaptorAliasResult(null, "Invalid syntax: " + command
						+ " \n" + getUsage());
			} else if (param.equalsIgnoreCase("remove")) {
				for (String user : usersWithControl) {
					controller.getConnector().sendMessage(
							"tell " + user
									+ " I have removed your spoof access.");
				}
				usersWithControl.clear();
				return new RaptorAliasResult(null,
						"All spoof access has been removed.");
			} else {
				usersWithControl.add(param);
				controller.getConnector().invokeOnNextMatch(
						param + " tells you\\: .*", new MessageCallback() {
							public boolean matchReceived(final ChatEvent event) {
								if (event.getType() != ChatType.TELL
										|| controller.isDisposed()) {
									return false;
								}
								boolean hasAccess = false;
								for (String user : usersWithControl) {
									if (user
											.equalsIgnoreCase(event.getSource())) {
										hasAccess = true;
										break;
									}
								}
								if (hasAccess) {
									String message = event.getMessage();
									RaptorStringTokenizer messageTok = new RaptorStringTokenizer(
											message, " ", true);
									messageTok.nextToken();
									messageTok.nextToken();
									messageTok.nextToken();

									message = messageTok.getWhatsLeft().trim();
									if (!message.startsWith("/")
											&& !message.startsWith("\\")) {
										final String finalMessage = StringUtils
												.replaceChars(message, "\\\n",
														"");
										Raptor.getInstance().getDisplay()
												.asyncExec(new Runnable() {
													public void run() {
														controller
																.onAppendChatEventToInputText(new ChatEvent(
																		null,
																		ChatType.INTERNAL,
																		event
																				.getSource()
																				+ " is spoofing: '"
																				+ finalMessage
																				+ "'. To kill his/her access type 'grantspoof remove'."));
													}
												});
										controller.getConnector().sendMessage(
												finalMessage, true);
									}
									return true;
								} else {
									return false;
								}
							}
						});

				return new RaptorAliasResult(
						"tell "
								+ param
								+ " You now have spoof access to my account. Every tell you send me will be executed "
								+ "as a command. If you send me a tell that starts with / or \\ it will not be exuected as a command.",
						"All tells sent to you from "
								+ param
								+ " will now be executed as commands. You can stop this at any time by "
								+ "typing 'grantspoof remove'. If the user sends you a tell that starts "
								+ "with / or \\ the command will not be executed.");
			}

		}
		return null;
	}
}