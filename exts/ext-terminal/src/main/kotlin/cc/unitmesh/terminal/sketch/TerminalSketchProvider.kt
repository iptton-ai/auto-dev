package cc.unitmesh.terminal.sketch

import cc.unitmesh.devti.AutoDevNotifications
import cc.unitmesh.devti.sketch.SketchToolWindow
import cc.unitmesh.devti.sketch.ui.ExtensionLangSketch
import cc.unitmesh.devti.sketch.ui.LanguageSketchProvider
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.util.MinimizeButton
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * TerminalSketch provide a support for `bash` and `shell` language in terminal.
 */
class TerminalSketchProvider : LanguageSketchProvider {
    override fun isSupported(lang: String): Boolean = lang == "bash" || lang == "shell"

    override fun create(project: Project, content: String): ExtensionLangSketch {
        var content = content
        return object : ExtensionLangSketch {
            var terminalWidget: JBTerminalWidget? = null
            var panelLayout: JPanel? = null

            private var titlePanel = JPanel(HorizontalLayout(JBUI.scale(10))).also {
                it.background = UIUtil.getPanelBackground()
            }

            init {
                val projectDir = project.guessProjectDir()?.path
                val terminalRunner = LocalTerminalDirectRunner.createTerminalRunner(project)
                terminalWidget = terminalRunner.createTerminalWidget(this, projectDir, true).also {
                    it.preferredSize = Dimension(it.preferredSize.width, 120)
                }

                panelLayout = object : JPanel(BorderLayout()) {
                    init {
                        titlePanel.layout = FlowLayout(FlowLayout.LEFT)

                        border = JBUI.Borders.customLine(UIUtil.getFocusedBorderColor(), 0, 0, 1, 0)

                        val titleLabel = JLabel("Terminal").apply {
                            border = JBUI.Borders.empty(5, 0)
                        }

                        val clearPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                            add(JButton("Clear").apply {
                                addMouseListener(object : MouseAdapter() {
                                    override fun mouseClicked(e: MouseEvent?) {
                                        terminalWidget?.terminalStarter?.sendString("clear\n", false)
                                    }
                                })
                            })
                        }

                        titlePanel.add(titleLabel)
                        titlePanel.add(Box.createHorizontalGlue()) // 推动按钮到右侧
                        titlePanel.add(clearPanel)

                        add(titlePanel, BorderLayout.NORTH)
                        add(terminalWidget!!.component, BorderLayout.CENTER)

                        val buttonPanel = JPanel(HorizontalLayout(JBUI.scale(10)))
                        val sendButton = JButton("Send").apply {
                            addMouseListener(object : MouseAdapter() {
                                override fun mouseClicked(e: MouseEvent?) {
                                    try {
                                        val output = terminalWidget!!::class.java.getMethod("getText")
                                            .invoke(terminalWidget) as String
                                        sendToSketch(project, output)
                                    } catch (e: Exception) {
                                        AutoDevNotifications.notify(project, "Failed to send to Sketch")
                                    }
                                }
                            })
                        }

                        val popupButton = JButton("Pop up Terminal").apply {
                            addMouseListener(executePopup(terminalWidget, project))
                        }

                        buttonPanel.add(sendButton)
                        buttonPanel.add(popupButton)
                        add(buttonPanel, BorderLayout.SOUTH)
                    }
                }

                panelLayout!!.border = JBUI.Borders.compound(
                    JBUI.Borders.empty(5, 10),
                )
            }


            private fun executePopup(terminalWidget: JBTerminalWidget?, project: Project): MouseAdapter =
                object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        var popup: JBPopup? = null
                        popup = JBPopupFactory.getInstance()
                            .createComponentPopupBuilder(terminalWidget!!.component, null)
                            .setProject(project)
                            .setResizable(true)
                            .setMovable(true)
                            .setTitle("Terminal")
                            .setCancelButton(MinimizeButton("Hide"))
                            .setCancelCallback {
                                popup?.cancel()
                                panelLayout!!.remove(terminalWidget.component)
                                panelLayout!!.add(terminalWidget.component)

                                panelLayout!!.revalidate()
                                panelLayout!!.repaint()
                                true
                            }
                            .setFocusable(true)
                            .setRequestFocus(true)
                            .createPopup()

                        val editor = FileEditorManager.getInstance(project).selectedTextEditor
                        if (editor != null) {
                            popup.showInBestPositionFor(editor)
                        } else {
                            popup.showInFocusCenter()
                        }
                    }
                }

            override fun getExtensionName(): String = "Terminal"
            override fun getViewText(): String = content
            override fun updateViewText(text: String) {
                content = text
            }

            override fun doneUpdateText(allText: String) {
                ApplicationManager.getApplication().invokeLater {
                    Thread.sleep(2000) // todo: change to when terminal ready
                    terminalWidget!!.terminalStarter?.sendString(content, false)
                }
            }

            override fun getComponent(): JComponent = panelLayout!!

            override fun updateLanguage(language: Language?, originLanguage: String?) {}

            override fun dispose() {}
        }
    }

    private fun sendToSketch(project: Project, output: String) {
        val contentManager = ToolWindowManager.getInstance(project).getToolWindow("AutoDev")?.contentManager
        contentManager?.component?.components?.filterIsInstance<SketchToolWindow>()?.firstOrNull().let {
            it?.sendInput(output)
        }
    }
}
