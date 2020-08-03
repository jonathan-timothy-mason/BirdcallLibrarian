package jonathan.mason.birdcalllibrarian;

import butterknife.BindView;
import butterknife.ButterKnife;
import jonathan.mason.birdcalllibrarian.Database.Birdcall;

import android.animation.Animator;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.List;

/**
 * RecyclerView adapter for providing birdcalls.
 */
public class BirdcallsAdapter extends RecyclerView.Adapter<BirdcallsAdapter.BirdcallViewHolder> {

    /**
     * Listener for notification of birdcall selection.
     */
    public interface BirdcallSelectionListener {
        /**
         * Handle selection of birdcall.
         * @param selectedBirdcall Selected birdcall.
         */
        void onBirdcallSelected(Birdcall selectedBirdcall);
    }

    private List<Birdcall> mBirdcalls;
    private BirdcallSelectionListener mBirdcallSelectionListener;

    /**
     * Constructor.
     * @param birdcalls Birdcalls to be supplied to RecyclerView.
     * @param birdcallSelectionListener Listener for notification of birdcall selection.
     */
    public BirdcallsAdapter(List<Birdcall> birdcalls, BirdcallSelectionListener birdcallSelectionListener)
    {
        mBirdcalls = birdcalls;
        mBirdcallSelectionListener = birdcallSelectionListener;
    }

    /**
     * Create instance of BirdcallViewHolder class.
     * @param parent Parent ViewGroup to which view holder is to be added.
     * @param viewType Unused item type.
     * @return Instance of BirdcallViewHolder class.
     */
    @NonNull
    @Override
    public BirdcallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Dynamically create layout for item.
        View birdcallItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.birdcall_item, parent, false);

        // Create ViewHolder.
        return new BirdcallViewHolder(birdcallItem);
    }

    /**
     * Bind supplied view holder to birdcall at specified position in birdcalls list.
     * @param holder View holder to bind.
     * @param position Position of birdcall.
     */
    @Override
    public void onBindViewHolder(@NonNull BirdcallViewHolder holder, int position) {
        holder.Bind(position);
    }

    /**
     * Get total number of birdcalls.
     * @return Total number of birdcalls.
     */
    @Override
    public int getItemCount() {
        return mBirdcalls.size();
    }

    /**
     * Get all birdcalls of adapter.
     * @return All birdcalls of adapter.
     */
    public List<Birdcall> getBirdcalls() { return mBirdcalls; }

    /**
     * A ViewHolder subclass suitable for displaying a birdcall item.
     */
    public class BirdcallViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.birdcall_title) TextView mTitle;
        @BindView(R.id.birdcall_date_and_time) TextView mDataAndTime;

        /**
         * Constructor.
         * @param itemView View corresponding to birdcall item, to be bound to view holder.
         */
        public BirdcallViewHolder(View itemView) {
            super(itemView);

            // Retrieve view(s).
            ButterKnife.bind(this, itemView);

            // Setup click listener.
            itemView.setOnClickListener(this);
        }

        /**
         * Bind view holder to birdcall at specified position in birdcalls list.
         * @param position Position of birdcall.
         */
        public void Bind(int position){
            Birdcall birdcall = mBirdcalls.get(position);
            mTitle.setText(birdcall.getTitle());
            mDataAndTime.setText(DateFormat.getInstance().format(birdcall.getDateAndTime()));
        }

        /**
         * Handle selection of view holder to perform selection animation and notify
         * BirdcallSelectionListener of adapter.
         */
        @Override
        public void onClick(View view) {
            // Respond to click by blanking list item and re-revealing it.
            // From "Creating Paper Transformations" of Lesson 2: Surfaces, Material
            // Design for Android Developers by James.
            Animator anim = ViewAnimationUtils.createCircularReveal(view, view.getWidth() / 2, view.getHeight() / 2, 0, (float)(view.getWidth() / 1.75));
            anim.setDuration(view.getResources().getInteger(R.integer.circular_reveal_milliseconds));
            anim.addListener(new Animator.AnimatorListener() {
                /**
                 * Must be implemented, but not used.
                 */
                @Override
                public void onAnimationStart(Animator animation) { }

                /**
                 * Once animation has finished, call listener.
                 *
                 * @param animation Completed animation.
                 */
                @Override
                public void onAnimationEnd(Animator animation) {
                    int position = getAdapterPosition();
                    mBirdcallSelectionListener.onBirdcallSelected(mBirdcalls.get(position));
                    view.setBackgroundColor(Color.TRANSPARENT); // Reset background colour.
                }

                /**
                 * Must be implemented, but not used.
                 */
                @Override
                public void onAnimationCancel(Animator animation) { }

                /**
                 * Must be implemented, but not used.
                 */
                @Override
                public void onAnimationRepeat(Animator animation) { }
            });
            view.setBackgroundColor(view.getResources().getColor(R.color.colorAccent, null)); // New background colour to be revealed.
            anim.start();
        }
    }
}
