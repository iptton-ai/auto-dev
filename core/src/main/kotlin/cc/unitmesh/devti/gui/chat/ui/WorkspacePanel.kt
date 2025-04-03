package cc.unitmesh.devti.gui.chat.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel

class WorkspacePanel(
    private val project: Project,
    private val input: AutoDevInput
) : JPanel(BorderLayout()) {
    private val workspaceFiles = mutableListOf<VirtualFile>()
    private val filesPanel = JPanel(WrapLayout(FlowLayout.LEFT, 2, 2))
    
    init {
        border = JBUI.Borders.empty()

        val addButton = JBLabel(AllIcons.General.Add)
        addButton.cursor = Cursor(Cursor.HAND_CURSOR)
        addButton.toolTipText = "Add files to workspace"
        addButton.border = JBUI.Borders.empty(2, 4)
        addButton.background = JBColor(0xEDF4FE, 0x313741)
        addButton.isOpaque = true
        addButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                addFile()
            }
        })
        
        filesPanel.isOpaque = false
        filesPanel.add(addButton)
        
        add(filesPanel, BorderLayout.NORTH)
        isOpaque = false
    }
    
    private fun addFile() {
        val descriptor = FileChooserDescriptor(true, true, false, false, false, true)
            .withTitle("Select Files for Workspace")
            .withDescription("Choose files to add to your workspace")
        
        FileChooser.chooseFiles(descriptor, project, null) { files ->
            for (file in files) {
                addFileToWorkspace(file)
            }
        }
    }
    
    fun addFileToWorkspace(file: VirtualFile) {
        if (!workspaceFiles.contains(file)) {
            workspaceFiles.add(file)
            updateFilesPanel()

            val relativePath = try {
                project.basePath?.let { basePath ->
                    file.path.substringAfter(basePath).removePrefix("/")
                } ?: file.path
            } catch (e: Exception) {
                file.path
            }
            
            input.appendText("\n/file:$relativePath")
        }
    }
    
    private fun updateFilesPanel() {
        filesPanel.removeAll()
        
        val addButton = JBLabel(AllIcons.General.Add)
        addButton.cursor = Cursor(Cursor.HAND_CURSOR)
        addButton.toolTipText = "Add files to workspace"
        addButton.border = JBUI.Borders.empty(2, 4)
        addButton.background = JBColor(0xEDF4FE, 0x313741)
        addButton.isOpaque = true
        addButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                addFile()
            }
        })
        filesPanel.add(addButton)
        
        for (file in workspaceFiles) {
            val fileLabel = FileItemPanel(project, file) { 
                removeFile(file)
            }
            filesPanel.add(fileLabel)
        }
        
        filesPanel.revalidate()
        filesPanel.repaint()
    }
    
    private fun removeFile(file: VirtualFile) {
        workspaceFiles.remove(file)
        updateFilesPanel()
    }
    
    fun clear() {
        workspaceFiles.clear()
        updateFilesPanel()
    }
}

class FileItemPanel(
    private val project: Project,
    private val file: VirtualFile,
    private val onRemove: () -> Unit
) : JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)) {
    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(1, 3)
        )
        background = JBColor(0xEDF4FE, 0x313741)
        isOpaque = true
        
        val icon = file.fileType.icon
        val fileLabel = JBLabel(file.name, icon, JBLabel.LEFT)
        
        val removeLabel = JBLabel(AllIcons.Actions.Close)
        removeLabel.cursor = Cursor(Cursor.HAND_CURSOR)
        removeLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onRemove()
            }
        })
        
        add(fileLabel)
        add(removeLabel)
        
        this.border = JBUI.Borders.empty(2)
    }
}

/**
 * FlowLayout subclass that fully supports wrapping of components.
 */
class WrapLayout : FlowLayout {
    constructor() : super()
    constructor(align: Int) : super(align)
    constructor(align: Int, hgap: Int, vgap: Int) : super(align, hgap, vgap)

    /**
     * Returns the preferred dimensions for this layout given the components
     * in the specified target container.
     * @param target the container that needs to be laid out
     * @return the preferred dimensions to lay out the subcomponents of the specified container
     */
    override fun preferredLayoutSize(target: Container): Dimension {
        return layoutSize(target, true)
    }

    /**
     * Returns the minimum dimensions needed to layout the components
     * contained in the specified target container.
     * @param target the container that needs to be laid out
     * @return the minimum dimensions to lay out the subcomponents of the specified container
     */
    override fun minimumLayoutSize(target: Container): Dimension {
        return layoutSize(target, false)
    }

    /**
     * Calculate the dimensions needed to layout the components in the target container
     * @param target the target container
     * @param preferred true for preferred size, false for minimum size
     * @return the dimensions needed for layout
     */
    private fun layoutSize(target: Container, preferred: Boolean): Dimension {
        synchronized(target.treeLock) {
            // Each row must fit within the target container width
            var targetWidth = target.width
            
            if (targetWidth == 0) {
                targetWidth = Int.MAX_VALUE
            }

            val hgap = this.hgap
            val vgap = this.vgap
            val insets = target.insets
            val horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2)
            val maxWidth = targetWidth - horizontalInsetsAndGap

            // Fit components into the calculated width
            var dim = Dimension(0, 0)
            var rowWidth = 0
            var rowHeight = 0

            val count = target.componentCount
            for (i in 0 until count) {
                val m = target.getComponent(i)
                if (m.isVisible) {
                    val d = if (preferred) m.preferredSize else m.minimumSize
                    
                    // If this component doesn't fit in the current row, start a new row
                    if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                        dim.width = maxWidth.coerceAtLeast(rowWidth)
                        dim.height += rowHeight + vgap
                        rowWidth = 0
                        rowHeight = 0
                    }

                    // Add component to current row
                    rowWidth += d.width + hgap
                    rowHeight = rowHeight.coerceAtLeast(d.height)
                }
            }

            // Add last row dimensions
            dim.width = maxWidth.coerceAtLeast(rowWidth)
            dim.height += rowHeight + vgap

            // Account for container's insets
            dim.width += horizontalInsetsAndGap
            dim.height += insets.top + insets.bottom + vgap * 2

            return dim
        }
    }
}
